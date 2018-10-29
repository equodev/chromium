#[cfg(feature = "gen")]
extern crate bindgen;
#[cfg(feature = "genJava")]
extern crate java_bindgen;

#[cfg(debug_assertions)]
const CEF_TARGET: &'static str = "Debug"; 
#[cfg(not(debug_assertions))]
const CEF_TARGET: &'static str = "Release";

#[cfg(feature = "gen")]
fn main() {
  link();
  let cef_path = get_cef_path();
  gen_cef(cef_path.display());
  gen_os(cef_path.display());
}

#[cfg(feature = "genJava")]
fn main() {
  link();
  let cef_path = get_cef_path();
  gen_java_cef(cef_path.display());
}

#[cfg(not(any(feature = "genJava", feature = "gen")))]
fn main() {
  link();
}

fn get_cef_path() -> std::path::PathBuf {
  let cwd = std::env::current_dir().unwrap();
  let mut cef_path = cwd.clone();
  
  if cfg!(target_os = "macos") {
    cef_path.push("cef_osx");
  } 
  else if cfg!(target_os = "linux") {
    cef_path.push("cef_linux");
  } 
  else if cfg!(target_os = "windows") {
    cef_path.push("cef_windows");
  }
  cef_path
}

fn link() {
  let cef_path = get_cef_path();

  if !cef_path.exists() {
    panic!("cargo:warning=Extract and rename cef binary (minimal) distro to {:?}", cef_path);
  }

  if cfg!(target_os = "linux") {
    println!("cargo:rustc-link-lib=gtk-x11-2.0");
    println!("cargo:rustc-link-lib=gdk-x11-2.0");
    println!("cargo:rustc-link-lib=X11");
  }

  // Tell cargo to tell rustc to link the system shared library.
  let mut cef_bin = cef_path.clone();
  cef_bin.push(CEF_TARGET);
  let lib = if cfg!(target_os = "windows") {
    println!("cargo:rustc-link-search={}", cef_bin.display()); 
    "libcef" 
  } else if cfg!(target_os = "macos") {
    println!("cargo:rustc-link-search=framework={}", cef_bin.display());
    "framework=Chromium Embedded Framework"
  } else { 
    println!("cargo:rustc-link-search={}", cef_bin.display());
    "cef" 
  };
  println!("cargo:rustc-link-lib={}", lib);
}

#[cfg(feature = "gen")]
#[cfg(target_os = "windows")]
fn gen_os(cef_path: std::path::Display) {
  let _ = generator(cef_path)
    .header("cef_win.h")
    .whitelist_type("_cef_main_args_t")
    .whitelist_type("_cef_window_info_t")
    .blacklist_type(".*string.*")
    .raw_line("use cef::cef_string_t;")
    .generate().expect("Failed to gencef win")
    .write_to_file(std::path::Path::new("src").join("cef").join("win.rs"));
}

#[cfg(feature = "gen")]
#[cfg(target_os = "linux")]
fn gen_os(cef_path: std::path::Display) {
  let _ = generator(cef_path)
    .header("cef_linux.h")
    .whitelist_type("_cef_main_args_t")
    .whitelist_type("_cef_window_info_t")
    .generate().expect("Failed to gencef linux")
    .write_to_file(std::path::Path::new("src").join("cef").join("linux.rs"));
}

#[cfg(feature = "gen")]
#[cfg(target_os = "macos")]
fn gen_os(cef_path: std::path::Display) {
  let _ = generator(cef_path)
    .header("cef_mac.h")
    .whitelist_type("_cef_main_args_t")
    .whitelist_type("_cef_window_info_t")
    .blacklist_type(".*string.*")
    .raw_line("use cef::cef_string_t;")
    .generate().expect("Failed to gencef mac")
    .write_to_file(std::path::Path::new("src").join("cef").join("mac.rs"));
}

#[cfg(feature = "gen")]
fn gen_cef(cef_path: std::path::Display) {
  let _ = generator(cef_path)
    .header("cef.h")
    .whitelist_type("cef_string_t")
    .whitelist_type(".*cef_base_t")
    .whitelist_type("_cef_scheme_registrar_t")
    .whitelist_type("_cef_.*_handler_t")
    .whitelist_type("_cef_urlrequest_client_t")
    .whitelist_type("_cef_urlrequest_t")
    .whitelist_function("cef_string_.*")
    .whitelist_function("cef_execute_process")
    .whitelist_function("cef_initialize")
    .whitelist_function("cef_run_message_loop")
    .whitelist_function("cef_shutdown")
    .whitelist_function("cef_browser_host_create_browser")
    .whitelist_function("cef_urlrequest_create")
    .whitelist_function("cef_cookie_manager_get_global_manager")
    .whitelist_function("cef_.*")
    .blacklist_type("_cef_main_args_t")
    .blacklist_type("_cef_window_info_t")
    .raw_line("#[cfg(target_os = \"linux\")] pub mod linux;")
    .raw_line("#[cfg(target_os = \"linux\")] pub use self::linux::_cef_window_info_t;")
    .raw_line("#[cfg(target_os = \"linux\")] pub use self::linux::_cef_main_args_t;")
    .raw_line("#[cfg(target_os = \"macos\")] pub mod mac;")
    .raw_line("#[cfg(target_os = \"macos\")] pub use self::mac::_cef_window_info_t;")
    .raw_line("#[cfg(target_os = \"macos\")] pub use self::mac::_cef_main_args_t;")
    .raw_line("#[cfg(windows)] pub mod win;")
    .raw_line("#[cfg(windows)] pub use self::win::_cef_window_info_t;")
    .raw_line("#[cfg(windows)] pub use self::win::_cef_main_args_t;")
    .generate().expect("Failed to gencef")
    .write_to_file(std::path::Path::new("src").join("cef").join("mod.rs"));
}

#[cfg(feature = "genJava")]
fn gen_java_cef(cef_path: std::path::Display) {
  let config = java_bindgen::CodegenConfig {
            functions: true,
            types: true,
            vars: false,
            methods: true,
            constructors: false,
            destructors: false
        };
  let gen = java_bindgen::builder()
    .clang_arg(format!("-I{}", cef_path))
    .clang_arg(format!("-I{}", "C:\\Program Files (x86)\\Microsoft SDKs\\Windows\\v7.1A\\Include"))
    .clang_arg("-fparse-all-comments")
    .clang_arg("-Wno-nonportable-include-path")
    .clang_arg("-Wno-invalid-token-paste")
//    .link("cef")
    //.use_core()
    .with_codegen_config(config);
  let _ = gen
    .header("cef_java.h")
    .whitelist_recursively(false)
    .whitelist_type("cef_string_t")
    .whitelist_type("_cef_string_utf16_t")
    .whitelist_type("cef_string_userfree_t")
    .whitelist_type("cef_string_userfree_utf16_t")
    // .whitelist_type("_cef_size_t")
    // .whitelist_type("_cef_rect_t")
    // .whitelist_type("_cef_range_t")
    // .whitelist_type("cef_color_model_t")
    // .whitelist_type("cef_duplex_mode_t")
    .whitelist_type("cef_focus_source_t")
    .whitelist_type("cef_process_id_t")
    .whitelist_type("cef_window_open_disposition_t")
    .whitelist_type("cef_transition_type_t")
    .whitelist_type("cef_errorcode_t")
    .whitelist_type("cef_termination_status_t")
    .whitelist_type("cef_urlrequest_status_t")
    .whitelist_type("cef_return_value_t")
    // .whitelist_type("_cef_browser_t")
    // .whitelist_type("_cef_browser_host_t")
    // .whitelist_type("_cef_frame_t")
    // .whitelist_type("_cef_process_message_t")
    .whitelist_type("_cef_browser_process_handler_t")
    .whitelist_type("_cef_client_t")
    .whitelist_type("_cef_focus_handler_t")
    .whitelist_type("_cef_app_t")
    .whitelist_type("_cef_life_span_handler_t")
    .whitelist_type("_cef_load_handler_t")
    .whitelist_type("_cef_display_handler_t")
    .whitelist_type("_cef_request_handler_t")
    .whitelist_type("_cef_string_visitor_t")
    .whitelist_type("_cef_cookie_visitor_t")
    .whitelist_type("_cef_cookie_t")
    .whitelist_type("_cef_time_t")
    // .whitelist_type("_cef_command_line_t")
    // .whitelist_type("_cef_print_handler_t")
    // .whitelist_type("_cef_print_settings_t")
    // .whitelist_type("_cef_print_dialog_callback_t")
    // .whitelist_type("_cef_print_job_callback_t")
    .opaque_type("_cef_list_value_t")
    .blacklist_type("_cef_base_ref_counted_t")
    .opaque_type("_cef_print_handler_t")
    .opaque_type("_cef_command_line_t")
    .blacklist_type("_cef_command_line_t")
    .opaque_type("_cef_scheme_registrar_t")
    .opaque_type("_cef_resource_bundle_handler_t")
    .opaque_type("_cef_render_process_handler_t")
    .opaque_type("_cef_browser_t")
    .opaque_type("_cef_process_message_t")
    .opaque_type("_cef_render_handler_t")
    .opaque_type("_cef_keyboard_handler_t")
    .opaque_type("_cef_jsdialog_handler_t")
    .opaque_type("_cef_geolocation_handler_t")
    .opaque_type("_cef_find_handler_t")
    .opaque_type("_cef_drag_handler_t")
    .opaque_type("_cef_download_handler_t")
    .opaque_type("_cef_dialog_handler_t")
    .opaque_type("_cef_context_menu_handler_t")
    .opaque_type("_cef_frame_t")
    .whitelist_type("_cef_popup_features_t")
    .opaque_type("_cef_window_info_t")
    .opaque_type("_cef_browser_settings_t")
    .opaque_type("_cef_x509certificate_t")
    .opaque_type("_cef_select_client_certificate_callback_t")
    .opaque_type("_cef_sslinfo_t")
    .opaque_type("_cef_auth_callback_t")
    .opaque_type("_cef_request_t")
    .opaque_type("_cef_request_callback_t")
    .opaque_type("_cef_response_t")
    .opaque_type("_cef_response_filter_t")
    .opaque_type("_cef_resource_handler_t")
    // .whitelisted_type(".*cef_base_t")
    // .whitelisted_type("_cef_scheme_registrar_t")
    // .whitelisted_type("_cef_.*_handler_t")
    // .whitelisted_function("cef_string_.*")
    // .whitelisted_function("cef_execute_process")
    // .whitelisted_function("cef_initialize")
    // .whitelisted_function("cef_run_message_loop")
    // .whitelisted_function("cef_shutdown")
    // .whitelisted_function("cef_browser_host_create_browser")
    // .whitelisted_function("cef_.*")
    // .blacklist_type("_cef_main_args_t")
    // .blacklist_type("_cef_window_info_t")
    .raw_line("package org.eclipse.swt.internal.chromium;")
    .raw_line("import org.eclipse.swt.internal.chromium.CEFFactory.cef_base_ref_counted_t;")
    .raw_line("import static org.eclipse.swt.internal.chromium.CEFFactory.mapTypeForClosure;")
    .raw_line("import jnr.ffi.*;")
    .raw_line("import jnr.ffi.annotations.*;")
    .raw_line("import jnr.ffi.byref.*;")
    .raw_line("import jnr.ffi.util.EnumMapper.IntegerEnum;")
    .enable_cxx_namespaces()
    .disable_name_namespacing()
    .ctypes_prefix("")
    .layout_tests(false)
    .rustified_enum(".*")
    .rustfmt_bindings(false)
    .generate().expect("Failed to gencef")
    .write_to_file(std::path::Path::new("target").join("CEF.java"));
}

#[cfg(feature = "gen")]
fn generator(cef_path: std::path::Display) -> bindgen::Builder {
  let config = bindgen::CodegenConfig {
            functions: true,
            types: true,
            vars: false,
            methods: true,
            constructors: false,
            destructors: false
        };
  let gen = bindgen::builder()
    .clang_arg(format!("-I{}", cef_path))
    .clang_arg(format!("-I{}", "C:\\Program Files (x86)\\Microsoft SDKs\\Windows\\v7.1A\\Include"))
    .clang_arg("-fparse-all-comments")
    .clang_arg("-Wno-nonportable-include-path")
    .clang_arg("-Wno-invalid-token-paste")
    .link("cef")
    //.use_core()
    .with_codegen_config(config)
    .rustified_enum(".*")
    .rustfmt_bindings(true)
    .derive_debug(true)
    .raw_line("#![allow(dead_code)]")
    .raw_line("#![allow(non_snake_case)]")
    .raw_line("#![allow(non_camel_case_types)]")
    .raw_line("#![allow(non_upper_case_globals)]");
  gen
} 