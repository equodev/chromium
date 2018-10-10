extern crate chromium;

#[cfg(target_os = "linux")]
extern crate x11;
#[cfg(unix)]
extern crate nix;
#[cfg(target_os = "macos")]
#[macro_use]
extern crate objc;

use chromium::cef;
use chromium::utils;
use chromium::socket;

mod app;
#[cfg(target_os = "linux")]
mod gtk2;

use std::os::raw::{c_char, c_int, c_ulong, c_void};
#[cfg(unix)]
use std::collections::HashMap;
use std::io::prelude::*;

#[cfg(target_os = "linux")]
unsafe extern fn xerror_handler_impl(_: *mut x11::xlib::Display, event: *mut x11::xlib::XErrorEvent) -> c_int {
    print!("X error received: ");
    println!("type {}, serial {}, error_code {}, request_code {}, minor_code {}", 
        (*event).type_, (*event).serial, (*event).error_code, (*event).request_code, (*event).minor_code);
    0
}
#[cfg(target_os = "linux")]
unsafe extern fn xioerror_handler_impl(_: *mut x11::xlib::Display) -> c_int {
    println!("XUI error received");
    0
}

#[no_mangle]
pub extern fn cefswt_init(japp: *mut cef::cef_app_t, cefrust_path: *const c_char, version: *const c_char) {
    println!("DLL init");
    assert_eq!(unsafe{(*japp).base.size}, std::mem::size_of::<cef::_cef_app_t>());
    //println!("app {:?}", japp);

    let cefrust_path = utils::str_from_c(cefrust_path);
    let version = utils::str_from_c(version);

    // let key = "LD_LIBRARY_PATH";
    // env::set_var(key, cefrust_path);

    let main_args = utils::prepare_args();

    let cefrust_dir = std::path::Path::new(&cefrust_path);

    // env::set_current_dir(cefrust_dir).expect("Failed to set current dir");
    // println!("{:?}", env::current_dir().unwrap().to_str());

    let subp = utils::subp_path(cefrust_dir, version);
    let subp_cef = utils::cef_string(&subp);
    
    let resources_cef = if cfg!(target_os = "macos") {
        utils::cef_string(cefrust_dir.join("Chromium Embedded Framework.framework").join("Resources").to_str().unwrap())
    } else {
        utils::cef_string(cefrust_dir.to_str().unwrap())
    };
    let locales_cef = if cfg!(target_os = "macos") {
        utils::cef_string(cefrust_dir.join("Chromium Embedded Framework.framework").join("Resources").to_str().unwrap())
    } else {
        utils::cef_string(cefrust_dir.join("locales").to_str().unwrap())
    };
    let framework_dir_cef = if cfg!(target_os = "macos") {
        utils::cef_string(cefrust_dir.join("Chromium Embedded Framework.framework").to_str().unwrap())
    } else {
        utils::cef_string_empty()
    };

    let cache_dir_cef = utils::cef_string(cefrust_dir.parent().unwrap().parent().unwrap().join("cef_cache").to_str().unwrap());

    let logfile_cef = utils::cef_string(cefrust_dir.join("lib.log").to_str().unwrap());

    let settings = cef::_cef_settings_t {
        size: std::mem::size_of::<cef::_cef_settings_t>(),
        single_process: 0,
        no_sandbox: 1,
        browser_subprocess_path: subp_cef,
        framework_dir_path: framework_dir_cef,
        multi_threaded_message_loop: 0,
        external_message_pump: 1,
        windowless_rendering_enabled: 0,
        command_line_args_disabled: 0,
        cache_path: cache_dir_cef,
        user_data_path: utils::cef_string_empty(),
        persist_session_cookies: 1,
        persist_user_preferences: 1,
        user_agent: utils::cef_string_empty(),
        product_version: utils::cef_string_empty(),
        locale: utils::cef_string_empty(),
        log_file: logfile_cef,
        log_severity: cef::cef_log_severity_t::LOGSEVERITY_INFO,
        // log_severity: cef::cef_log_severity_t::LOGSEVERITY_VERBOSE,
        javascript_flags: utils::cef_string_empty(),
        resources_dir_path: resources_cef,
        locales_dir_path: locales_cef,
        pack_loading_disabled: 0,
        remote_debugging_port: 0,
        uncaught_exception_stack_size: 0,
        ignore_certificate_errors: 0,
        enable_net_security_expiration: 0,
        background_color: 0,
        accept_language_list: utils::cef_string_empty()
    };

    println!("Calling cef_initialize");
    do_initialize(main_args, settings, japp);
}

#[cfg(target_os = "linux")]
fn do_initialize(main_args: cef::_cef_main_args_t, settings: cef::_cef_settings_t, app_raw: *mut cef::_cef_app_t) {
    unsafe { x11::xlib::XSetErrorHandler(Option::Some(xerror_handler_impl)) };
    unsafe { x11::xlib::XSetIOErrorHandler(Option::Some(xioerror_handler_impl)) };

    let mut signal_handlers: HashMap<c_int, nix::sys::signal::SigAction> = HashMap::new();
    backup_signal_handlers(&mut signal_handlers);
    
    unsafe { cef::cef_initialize(&main_args, &settings, app_raw, std::ptr::null_mut()) };

    restore_signal_handlers(signal_handlers);
}

#[cfg(target_os = "macos")]
static EVENT_KEY: char = 'k';

#[cfg(target_os = "macos")]
fn do_initialize(main_args: cef::_cef_main_args_t, settings: cef::_cef_settings_t, app_raw: *mut cef::_cef_app_t) {
    let mut signal_handlers: HashMap<c_int, nix::sys::signal::SigAction> = HashMap::new();
    backup_signal_handlers(&mut signal_handlers);
    
    swizzle_send_event();

    unsafe { cef::cef_initialize(&main_args, &settings, &mut (*app_raw), std::ptr::null_mut()) };

    restore_signal_handlers(signal_handlers);
}

#[cfg(target_os = "macos")]
fn swizzle_send_event() {
    use std::ffi::CString;
    use objc::runtime::{BOOL, Class, Method, NO, YES, Object, Sel, self};
    use objc::{Encode, EncodeArguments, Encoding};
    use nix::libc::intptr_t;

    fn count_args(sel: Sel) -> usize {
        sel.name().chars().filter(|&c| c == ':').count()
    }

    fn method_type_encoding(ret: &Encoding, args: &[Encoding]) -> CString {
        let mut types = ret.as_str().to_owned();
        // First two arguments are always self and the selector
        types.push_str(<*mut Object>::encode().as_str());
        types.push_str(Sel::encode().as_str());
        types.extend(args.iter().map(|e| e.as_str()));
        CString::new(types).unwrap()
    }
    
    pub unsafe fn add_method<F>(cls: *mut Class, sel: Sel, func: F)
            where F: objc::declare::MethodImplementation<Callee=Object> {
        let encs = F::Args::encodings();
        let encs = encs.as_ref();
        let sel_args = count_args(sel);
        assert!(sel_args == encs.len(),
            "Selector accepts {} arguments, but function accepts {}",
            sel_args, encs.len(),
        );

        let types = method_type_encoding(&F::Ret::encode(), encs);
        let success = runtime::class_addMethod(cls, sel, func.imp(),
            types.as_ptr());
        assert!(success != NO, "Failed to add method {:?}", sel);
    }

    pub type Id = *mut runtime::Object;
    pub type AssociationPolicy = intptr_t;
    extern {
        pub fn objc_getAssociatedObject(object: Id, key: *const c_void) -> BOOL;
        pub fn objc_setAssociatedObject(object: Id,
                                    key: *const c_void,
                                    value: BOOL,
                                    policy: AssociationPolicy);
    }

    let cls_nm = CString::new("NSApplication").unwrap();
    let cls = unsafe { runtime::objc_getClass(cls_nm.as_ptr()) as *mut Class };
    assert!(!cls.is_null(), "null class");

    extern fn is_handling_sendevent(this: &mut Object, _cmd: Sel) -> BOOL {
        //println!("isHandlingSendEvent {:?}", this);
        let kp = &EVENT_KEY as *const _ as *const c_void;
        let is = unsafe { objc_getAssociatedObject(this, kp) };
        //println!("AssociatedObject: {:?}", is);
        is
    }
    unsafe { add_method(cls, sel!(isHandlingSendEvent), is_handling_sendevent as extern fn(&mut Object, Sel) -> BOOL) };

    extern fn set_handling_sendevent(this: &mut Object, _cmd: Sel, handling_sendevent: BOOL) {
        //println!("setHandlingSendEvent {:?} {:?}", this, handling_sendevent);
        let kp = &EVENT_KEY as *const _ as *const c_void;
        let policy_assign = 0;
        unsafe { objc_setAssociatedObject(this, kp, handling_sendevent, policy_assign) };
    }
    unsafe { add_method(cls, sel!(setHandlingSendEvent:), set_handling_sendevent as extern fn(&mut Object, Sel, BOOL)) };

    extern fn swizzled_sendevent(this: &mut Object, _cmd: Sel, event: Id) {
        //println!("swizzled_sendevent {:?}", this);
        unsafe {
            let handling: BOOL = msg_send![this, isHandlingSendEvent];
            msg_send![this, setHandlingSendEvent:YES];
            msg_send![this, _swizzled_sendEvent:event];
            msg_send![this, setHandlingSendEvent:handling];
        }
    }
    let sel_swizzled_sendevent = sel!(_swizzled_sendEvent:);
    unsafe { add_method(cls, sel_swizzled_sendevent, swizzled_sendevent as extern fn(&mut Object, Sel, Id)) };
    
    unsafe {
        let original = runtime::class_getInstanceMethod(cls, sel!(sendEvent:)) as *mut Method;
        let swizzled = runtime::class_getInstanceMethod(cls, sel_swizzled_sendevent) as *mut Method;
        runtime::method_exchangeImplementations(original, swizzled);
    }
}

#[cfg(target_os = "windows")]
fn do_initialize(main_args: cef::_cef_main_args_t, settings: cef::_cef_settings_t, app_raw: *mut cef::_cef_app_t) {
    unsafe { cef::cef_initialize(&main_args, &settings, &mut (*app_raw), std::ptr::null_mut()) };
}

#[cfg(unix)]
fn backup_signal_handlers(signal_handlers: &mut HashMap<c_int, nix::sys::signal::SigAction>) {
    use nix::sys::signal;
    let signals_to_restore = [signal::SIGHUP, signal::SIGINT, signal::SIGQUIT, signal::SIGILL, 
        signal::SIGABRT, signal::SIGFPE, signal::SIGSEGV, signal::SIGALRM, signal::SIGTERM, 
        signal::SIGCHLD, signal::SIGBUS, signal::SIGTRAP, signal::SIGPIPE];
    
    for signal in &signals_to_restore {
        let sig_action = signal::SigAction::new(signal::SigHandler::SigDfl,
                                          signal::SaFlags::empty(),
                                          signal::SigSet::empty());
        let oldsigact = unsafe { signal::sigaction(*signal, &sig_action) };
        //println!("backup signal {:?}:{:?}", signal, "oldsigact.ok()");
        signal_handlers.insert(*signal as c_int, oldsigact.unwrap());
    }
}

#[cfg(unix)]
fn restore_signal_handlers(signal_handlers: HashMap<c_int, nix::sys::signal::SigAction>) {
    use nix::sys::signal;
    for (signal, sigact) in signal_handlers {
        //println!("restore signal {:?}:{:?}", signal, "sigact");
        unsafe { signal::sigaction(std::mem::transmute(signal), &sigact).unwrap() };
    }
}

unsafe extern "C" fn on_process_message_received(
            self_: *mut cef::_cef_client_t,
            browser: *mut cef::_cef_browser_t,
            source_process: cef::cef_process_id_t,
            message: *mut cef::_cef_process_message_t,
        ) -> c_int
{
    println!("RUST on_process_message_received {:?} {} {}", source_process, cef::cef_currently_on(cef::cef_thread_id_t::TID_UI), cef::cef_currently_on(cef::cef_thread_id_t::TID_IO));
    1
}
        
#[no_mangle]
pub extern fn cefswt_create_browser(hwnd: c_ulong, url: *const c_char, client: &mut cef::_cef_client_t, w: c_int, h: c_int) -> *const cef::cef_browser_t {
    assert_eq!((*client).base.size, std::mem::size_of::<cef::_cef_client_t>());

    // println!("hwnd: {}", hwnd);
    // (*client).on_process_message_received = Option::Some(on_process_message_received);
 
    let url = utils::str_from_c(url);
    // println!("url: {:?}", url);
    let browser = app::create_browser(hwnd, url, client, w, h);

    // let browser_host = get_browser_host(browser);
    // unsafe {
    //     (*browser_host).show_dev_tools.expect("no dev_tools")(browser_host, std::ptr::null_mut(), std::ptr::null_mut(), std::ptr::null_mut(), std::ptr::null_mut());
    // }
    browser
}

#[no_mangle]
pub extern fn cefswt_set_window_info_parent(window_info: *mut cef::_cef_window_info_t, client: *mut *mut cef::_cef_client_t, jclient: &mut cef::_cef_client_t, hwnd: c_ulong) {
    println!("cefswt_set_window_info_parent {:?} {}", window_info, hwnd);
    unsafe { (*client) = jclient };
    app::set_window_parent(window_info, hwnd);
}

#[no_mangle]
pub extern fn cefswt_do_message_loop_work() {
    unsafe { cef::cef_do_message_loop_work() };
}

#[no_mangle]
pub extern fn cefswt_free(obj: *mut cef::cef_browser_t) {
    //println!("freeing {:?}", obj);
    unsafe {
        assert_eq!((*obj).base.size, std::mem::size_of::<cef::_cef_browser_t>());

        let rls_fn = (*obj).base.release.expect("null release");
        // println!("call rls");
        let refs = rls_fn(obj as *mut cef::_cef_base_ref_counted_t);
        assert_eq!(refs, 1);
    }

    println!("freed");
}

#[no_mangle]
pub extern fn cefswt_resized(browser: *mut cef::cef_browser_t, width: i32, height: i32) {
    //println!("Calling resized {}:{}", width, height);
    
    let browser_host = get_browser_host(browser);
    let get_window_handle_fn = unsafe { (*browser_host).get_window_handle.expect("no get_window_handle") };
    let win_handle = unsafe { get_window_handle_fn(browser_host) };
    do_resize(win_handle, width, height);
}

#[cfg(target_os = "linux")]
fn do_resize(win_handle: c_ulong, width: i32, height: i32) {
    use x11::xlib;

    let xwindow = win_handle;
    let xdisplay = unsafe { cef::cef_get_xdisplay() };
    let mut changes = xlib::XWindowChanges {
        x: 0,
        y: 0,
        width: width,
        height: height,
        border_width: 0,
        sibling: 0,
        stack_mode: 0
    };
    unsafe { xlib::XConfigureWindow(std::mem::transmute(xdisplay), xwindow,
        (xlib::CWX | xlib::CWY | xlib::CWHeight | xlib::CWWidth) as u32, &mut changes) };
}

#[cfg(target_os = "macos")]
fn do_resize(_win_handle: c_ulong, _: i32, _: i32) {
    // handled by cocoa
}

#[cfg(target_family = "windows")]
fn do_resize(win_handle: c_ulong, width: i32, height: i32) {
    extern crate winapi;

    let x = 0;
    let y = 0;
    unsafe { winapi::um::winuser::SetWindowPos(win_handle as winapi::shared::windef::HWND, 
        std::ptr::null_mut(), x, y, width, height, winapi::um::winuser::SWP_NOZORDER) };
}

#[no_mangle]
pub extern fn cefswt_close_browser(browser: *mut cef::cef_browser_t) {
    let browser_host = get_browser_host(browser);
    let close_fn = unsafe { (*browser_host).close_browser.expect("null try_close_browser") };
    unsafe { close_fn(browser_host, 1) };
}

#[no_mangle]
pub extern fn cefswt_load_url(browser: *mut cef::cef_browser_t, url: *const c_char) {
    let url = utils::str_from_c(url);
    let url_cef = utils::cef_string(url);
    println!("url: {:?}", url);
    let get_frame = unsafe { (*browser).get_main_frame.expect("null get_main_frame") };
    let main_frame = unsafe { get_frame(browser) };
    let load_url = unsafe { (*main_frame).load_url.expect("null load_url") };
    unsafe { load_url(main_frame, &url_cef) };
}

#[no_mangle]
pub extern fn cefswt_get_url(browser: *mut cef::cef_browser_t) -> *mut c_char {
    let get_frame = unsafe { (*browser).get_main_frame.expect("null get_main_frame") };
    let main_frame = unsafe { get_frame(browser) };
    assert!(!main_frame.is_null());
    let get_url = unsafe { (*main_frame).get_url.expect("null get_url") };
    let url = unsafe { get_url(main_frame) };
    if url.is_null() {
        return std::ptr::null_mut();
    } else {
        let utf8 = unsafe { cef::cef_string_userfree_utf8_alloc() };
        unsafe { cef::cef_string_utf16_to_utf8((*url).str, (*url).length, utf8) };
        return unsafe {(*utf8).str};
    }
}

#[no_mangle]
pub extern fn cefswt_cefstring_to_java(cefstring: *mut cef::cef_string_t) -> *mut c_char {
    unsafe {
        if (*cefstring).length == 0 {
            println!("NULL STRING");
            return std::ptr::null_mut();
        }
    }
    let utf8 = unsafe { cef::cef_string_userfree_utf8_alloc() };
    unsafe { cef::cef_string_utf16_to_utf8((*cefstring).str, (*cefstring).length, utf8) };
    return unsafe {(*utf8).str};
}

#[no_mangle]
pub extern fn cefswt_request_to_java(request: *mut cef::cef_request_t) -> *mut c_char {
    let url = unsafe { (*request).get_url.expect("null get_url")(request) };
    let cstr = cefswt_cefstring_to_java(url);
    unsafe { cef::cef_string_userfree_utf16_free(url) };
    cstr
}

#[no_mangle]
pub extern fn cefswt_load_text(browser: *mut cef::cef_browser_t, text: *const c_char) {
    let text = utils::str_from_c(text);
    let text_cef = utils::cef_string(text);
    let url_cef = utils::cef_string("http://text/");
    // println!("text: {:?}", text);
    let get_frame = unsafe { (*browser).get_main_frame.expect("null get_main_frame") };
    let main_frame = unsafe { get_frame(browser) };
    let load_string = unsafe { (*main_frame).load_string.expect("null load_string") };
    unsafe { load_string(main_frame, &text_cef, &url_cef) };
}

#[no_mangle]
pub extern fn cefswt_stop(browser: *mut cef::cef_browser_t) {
    unsafe { (*browser).stop_load.expect("null stop_load")(browser); };
}

#[no_mangle]
pub extern fn cefswt_reload(browser: *mut cef::cef_browser_t) {
    unsafe { (*browser).reload.expect("null reload")(browser); };
}

#[no_mangle]
pub extern fn cefswt_get_text(browser: *mut cef::cef_browser_t, visitor: *mut cef::_cef_string_visitor_t) {
    assert_eq!(unsafe{(*visitor).base.size}, std::mem::size_of::<cef::_cef_string_visitor_t>());
    let get_frame = unsafe { (*browser).get_main_frame.expect("null get_main_frame") };
    let main_frame = unsafe { get_frame(browser) };
    assert!(!main_frame.is_null());
    let get_text = unsafe { (*main_frame).get_source.expect("null get_text") };
    // println!("before get_text");
    unsafe { get_text(main_frame, visitor) };
    // println!("after get_text");
}

#[no_mangle]
pub extern fn cefswt_execute(browser: *mut cef::cef_browser_t, text: *const c_char) {
    let text = utils::str_from_c(text);
    let text_cef = utils::cef_string(text);
    let url_cef = utils::cef_string_empty();
    let get_frame = unsafe { (*browser).get_main_frame.expect("null get_main_frame") };
    let main_frame = unsafe { get_frame(browser) };
    let execute = unsafe { (*main_frame).execute_java_script.expect("null execute_java_script") };
    unsafe { execute(main_frame, &text_cef, &url_cef, 0) };
}

#[no_mangle]
pub extern fn cefswt_eval(browser: *mut cef::cef_browser_t, text: *const c_char, id: i32) -> c_int {
    let text_cef = utils::cef_string_from_c(text);
    let name = utils::cef_string("eval");
    unsafe {
        let msg = cef::cef_process_message_create(&name);
        let args = (*msg).get_argument_list.unwrap()(msg);
        let s = (*args).set_int.unwrap()(args, 0, id);
        assert_eq!(s, 1);
        let s = (*args).set_string.unwrap()(args, 1, &text_cef);
        assert_eq!(s, 1);
        let sent = (*browser).send_process_message.unwrap()(browser, cef::cef_process_id_t::PID_RENDERER, msg);
        assert_eq!(sent, 1);

        let r = socket::socket_server();

        sent
    }
}

#[no_mangle]
pub extern fn cefswt_function(browser: *mut cef::cef_browser_t, name: *const c_char, id: i32) -> c_int {
    let name_cef = utils::cef_string_from_c(name);
    let msg_name = utils::cef_string("function");
    unsafe {
        let msg = cef::cef_process_message_create(&msg_name);
        let args = (*msg).get_argument_list.unwrap()(msg);
        let s = (*args).set_int.unwrap()(args, 0, id);
        assert_eq!(s, 1);
        let s = (*args).set_string.unwrap()(args, 1, &name_cef);
        assert_eq!(s, 1);
        let sent = (*browser).send_process_message.unwrap()(browser, cef::cef_process_id_t::PID_RENDERER, msg);
        assert_eq!(sent, 1);
        sent
    }
}

#[no_mangle]
pub extern fn cefswt_function_return(browser: *mut cef::cef_browser_t, id: i32, ret: *const c_char) -> c_int {
    let ret_cef = utils::cef_string_from_c(ret);
    let msg_name = utils::cef_string("function_return");
    unsafe {
        let msg = cef::cef_process_message_create(&msg_name);
        let args = (*msg).get_argument_list.unwrap()(msg);
        let s = (*args).set_int.unwrap()(args, 0, id);
        assert_eq!(s, 1);
        let s = (*args).set_string.unwrap()(args, 1, &ret_cef);
        assert_eq!(s, 1);
        // let sent = (*browser).send_process_message.unwrap()(browser, cef::cef_process_id_t::PID_RENDERER, msg);
        // assert_eq!(sent, 1);
        // sent
        
        socket::socket_client()
    }
}

#[no_mangle]
pub extern fn cefswt_set_focus(browser: *mut cef::cef_browser_t, set: bool, parent: *mut c_void) {
    let browser_host = get_browser_host(browser);
    let focus_fn = unsafe { (*browser_host).set_focus.expect("null set_focus") };
    let focus = if set {
        1
    } else {
        0
    };
    println!("<<<<<<<< set_focus {}", focus);
    unsafe { focus_fn(browser_host, focus) };
    if !set && parent as c_ulong != 0 {
        do_set_focus(parent, focus);
    }
}

#[cfg(target_os = "linux")]
fn do_set_focus(parent: *mut c_void, focus: i32) {
    let root = unsafe { gtk2::gtk_widget_get_toplevel(parent) };
    println!("<<<<<<<< set_focus {} {:?} {:?}", focus, parent, root);
    // workaround to actually remove focus from cef inputs
    unsafe { gtk2::gtk_window_present(root) };
}

#[cfg(target_family = "windows")]
fn do_set_focus(_parent: *mut c_void, _focus: i32) {
    // TODO
}

#[cfg(target_os = "macos")]
fn do_set_focus(_parent: *mut c_void, _focus: i32) {
    // handled by cocoa
}

#[no_mangle]
pub extern fn cefswt_set_cookie(jurl: *const c_char, jname: *const c_char, jvalue: *const c_char, jdomain: *const c_char, jpath: *const c_char, secure: i32, httponly: i32, max_age: f64) -> c_int {
    let manager = unsafe { cef::cef_cookie_manager_get_global_manager(std::ptr::null_mut()) };
    let url = utils::cef_string_from_c(jurl);
    let domain = utils::cef_string_from_c(jdomain);
    let path = utils::cef_string_from_c(jpath);
    let name = utils::cef_string_from_c(jname);
    let value = utils::cef_string_from_c(jvalue);
    let has_expires = if max_age == -1.0 {
        0
    } else {
        1
    };
    let mut expires = cef::cef_time_t { year: 0, month: 0, day_of_week: 0, day_of_month: 0, hour: 0, minute: 0, second: 0, millisecond: 0 };
    
    if max_age == -1.0 {
        unsafe { cef::cef_time_from_doublet(max_age, &mut expires) };
    }

    let cookie = cef::_cef_cookie_t {
        name: name,
        value: value,
        domain: domain,
        path: path,
        secure,
        httponly,
        has_expires,
        expires,
        creation: expires,
        last_access: expires
    };
    unsafe { (*manager).set_cookie.expect("null set_cookie")(manager, &url, &cookie, std::ptr::null_mut()) }
}

#[no_mangle]
pub extern fn cefswt_get_cookie(jurl: *const c_char, jvisitor: *mut cef::_cef_cookie_visitor_t) -> c_int {
    let manager = unsafe { cef::cef_cookie_manager_get_global_manager(std::ptr::null_mut()) };
    let url = utils::cef_string_from_c(jurl);

    unsafe { (*manager).visit_url_cookies.expect("null visit_url_cookies")(manager, &url, 1, jvisitor) }
}

#[no_mangle]
pub extern fn cefswt_cookie_value(cookie: *mut cef::_cef_cookie_t) -> *mut c_char {
    unsafe { cefswt_cefstring_to_java(&mut (*cookie).value) }
}

#[no_mangle]
pub extern fn cefswt_delete_cookies() {
    let manager = unsafe { cef::cef_cookie_manager_get_global_manager(std::ptr::null_mut()) };
    unsafe { (*manager).delete_cookies.expect("null delete_cookies")(manager, std::ptr::null_mut(), std::ptr::null_mut(), std::ptr::null_mut()) };
}

#[no_mangle]
pub extern fn cefswt_shutdown() {
    println!("r: Calling cef_shutdown");
    // Shut down CEF.
    unsafe { cef::cef_shutdown() };
    // println!("r: After Calling cef_shutdown");
}

fn get_browser_host(browser: *mut cef::cef_browser_t) -> *mut cef::_cef_browser_host_t {
    let get_host_fn = unsafe { (*browser).get_host.expect("null get_host") };
    let browser_host = unsafe { get_host_fn(browser) };
    browser_host
}

#[no_mangle]
pub extern fn cefswt_is_main_frame(frame: *mut cef::_cef_frame_t) -> i32 {
    unsafe { (*frame).is_main.expect("null is_main")(frame) }
}

#[no_mangle]
pub extern fn cefswt_go_forward(browser: *mut cef::_cef_browser_t) {
    unsafe { (*browser).go_forward.expect("null go_forward")(browser) };
}

#[no_mangle]
pub extern fn cefswt_go_back(browser: *mut cef::_cef_browser_t) {
    unsafe { (*browser).go_back.expect("null go_back")(browser) };
}