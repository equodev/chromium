use cef;
use utils;
#[cfg(target_os = "linux")]
use gtk2;

use std::os::raw::{c_void, c_ulong, c_int, c_uint};
use std::mem::{size_of};
use std::ptr::{null_mut};

pub fn create_browser(canvas_hwnd: c_ulong, url: &str, jclient: &mut cef::_cef_client_t, w: c_int, h: c_int) -> *const cef::cef_browser_t {
    println!("create_browser in {}", canvas_hwnd);

    let window_info = cef_window_info(canvas_hwnd, w, h);
    // Browser settings.
    // It is mandatory to set the "size" member.
    let browser_settings = cef::_cef_browser_settings_t {
        size: size_of::<cef::_cef_browser_settings_t>(),
        windowless_frame_rate: 0,
        standard_font_family: utils::cef_string_empty(),
        fixed_font_family: utils::cef_string_empty(),
        serif_font_family: utils::cef_string_empty(),
        sans_serif_font_family: utils::cef_string_empty(),
        cursive_font_family: utils::cef_string_empty(),
        fantasy_font_family: utils::cef_string_empty(),
        default_font_size: 0,
        default_fixed_font_size: 0,
        minimum_font_size: 0,
        minimum_logical_font_size: 0,
        default_encoding: utils::cef_string_empty(),
        remote_fonts: cef::cef_state_t::STATE_DEFAULT,
        javascript: cef::cef_state_t::STATE_DEFAULT,
        javascript_open_windows: cef::cef_state_t::STATE_DEFAULT,
        javascript_close_windows: cef::cef_state_t::STATE_DEFAULT,
        javascript_access_clipboard: cef::cef_state_t::STATE_DEFAULT,
        javascript_dom_paste: cef::cef_state_t::STATE_DEFAULT,
        plugins: cef::cef_state_t::STATE_DEFAULT,
        universal_access_from_file_urls: cef::cef_state_t::STATE_DEFAULT,
        file_access_from_file_urls: cef::cef_state_t::STATE_DEFAULT,
        web_security: cef::cef_state_t::STATE_DEFAULT,
        image_loading: cef::cef_state_t::STATE_DEFAULT,
        image_shrink_standalone_to_fit: cef::cef_state_t::STATE_DEFAULT,
        text_area_resize: cef::cef_state_t::STATE_DEFAULT,
        tab_to_links: cef::cef_state_t::STATE_DEFAULT,
        local_storage: cef::cef_state_t::STATE_DEFAULT,
        databases: cef::cef_state_t::STATE_DEFAULT,
        application_cache: cef::cef_state_t::STATE_DEFAULT,
        webgl: cef::cef_state_t::STATE_DEFAULT,
        background_color: 0,
        accept_language_list: utils::cef_string_empty()
    };

    let url_cef = utils::cef_string(url);

    // Create browser.
    println!("Calling cef_browser_host_create_browser");
    //if unsafe { cef::cef_browser_host_create_browser(&window_info, client, &url_cef, &browser_settings, null_mut()) } != 1 {
        //println!("Failed calling browserHostCreateBrowser");
    //}
    let browser: *mut cef::cef_browser_t = unsafe { cef::cef_browser_host_create_browser_sync(&window_info, jclient, &url_cef, &browser_settings, null_mut()) };
    assert_eq!(unsafe{(*browser).base.size}, size_of::<cef::_cef_browser_t>());
    browser
}


#[cfg(target_os = "linux")]
fn cef_window_info(hwnd: c_ulong, w: c_int, h: c_int) -> cef::_cef_window_info_t {
    let window_info = cef::_cef_window_info_t {
        x: 0,
        y: 0,
        width: w as c_uint,
        height: h as c_uint,
        parent_window: unsafe {gtk2::gdk_x11_drawable_get_xid(gtk2::gtk_widget_get_window(hwnd as *mut c_void))},
        windowless_rendering_enabled: 0,
        window: 0
    };
    println!("parent {}", window_info.parent_window);
    window_info
}

#[cfg(target_os = "macos")]
fn cef_window_info(hwnd: c_ulong, w: c_int, h: c_int) -> cef::_cef_window_info_t {
    let window_info = cef::_cef_window_info_t {
        x: 0,
        y: 0,
        width: w,
        height: h,
        parent_view: hwnd as *mut c_void,
        windowless_rendering_enabled: 0,
        view: 0 as *mut c_void,
        hidden: 0,
        window_name: cef::cef_string_t { str: null_mut(),  length: 0,  dtor: Option::None }
    };
    println!("parent {:?}", window_info.parent_view);
    window_info
}

#[cfg(windows)]
fn cef_window_info(hwnd: c_ulong, w: c_int, h: c_int) -> cef::_cef_window_info_t {
    extern crate winapi;

    let window_info = cef::_cef_window_info_t {
        x: 0,
        y: 0,
        width: w,
        height: h,
        parent_window: hwnd as cef::win::HWND,
        windowless_rendering_enabled: 0,
        window: 0 as cef::win::HWND,
        ex_style: 0,
        window_name: cef::cef_string_t { str: null_mut(),  length: 0,  dtor: Option::None },
        style: winapi::um::winuser::WS_CHILDWINDOW | winapi::um::winuser::WS_CLIPCHILDREN
            | winapi::um::winuser::WS_CLIPSIBLINGS | winapi::um::winuser::WS_VISIBLE | winapi::um::winuser::WS_TABSTOP,
        menu: 0 as cef::win::HMENU
    };
    println!("parent {:?}", window_info.parent_window);
    window_info
}