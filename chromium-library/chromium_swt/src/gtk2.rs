#![allow(non_camel_case_types)]

use std::os::raw::{c_void, c_ulong};

pub type XID = c_ulong;

extern "C" {
    pub fn gtk_widget_get_window(widget: *mut c_void) -> *mut c_void;
    pub fn gdk_x11_drawable_get_xid(drawable: *mut c_void) -> XID;
    pub fn gtk_window_present(widget: *mut c_void);
    pub fn gtk_widget_get_toplevel(widget: *mut c_void) -> *mut c_void;
}
