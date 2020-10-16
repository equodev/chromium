#![allow(non_camel_case_types)]

use std::os::raw::{c_void, c_ulong, c_int};

pub type XID = c_ulong;

extern crate libloading as lib;

const LIBGTK3: &str = "libgtk-3.so.0";
const LIBGTK2: &str = "libgtk-x11-2.0.so.0";

pub static mut GTK3: bool = true;


fn gtk_lib() -> &'static str {
  if unsafe { GTK3 } { LIBGTK3 } else { LIBGTK2 }
}

pub fn gtk_widget_get_window(widget: *mut c_void) -> *mut c_void {
  let lib = lib::Library::new(gtk_lib()).expect("failed to load gtk");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> *mut c_void> = lib.get(b"gtk_widget_get_window").expect("failed to load gtk3 fn");
      func(widget)
  }
}

pub fn gdk_x11_window_get_xid(window: *mut c_void) -> XID {
  let lib = lib::Library::new(LIBGTK3).expect("failed to load gtk3");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> XID> = lib.get(b"gdk_x11_window_get_xid").expect("failed to load gtk3 fn");
      func(window)
  }
}

pub fn gdk_x11_drawable_get_xid(window: *mut c_void) -> XID {
  let lib = lib::Library::new(LIBGTK2).expect("failed to load gtk2");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> XID> = lib.get(b"gdk_x11_drawable_get_xid").expect("failed to load gtk2 fn");
      func(window)
  }
}

pub fn gtk_window_present(widget: *mut c_void) {
  let lib = lib::Library::new(gtk_lib()).expect("failed to load gtk");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void)> = lib.get(b"gtk_window_present").expect("failed to load gtk fn");
      func(widget)
  }
}

pub fn gtk_widget_get_toplevel(widget: *mut c_void) -> *mut c_void {
  let lib = lib::Library::new(gtk_lib()).expect("failed to load gtk");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> *mut c_void> = lib.get(b"gtk_widget_get_toplevel").expect("failed to load gtk fn");
      func(widget)
  }
}

pub fn gtk_widget_get_visual(window: *mut c_void) -> *mut c_void {
  let lib = lib::Library::new(gtk_lib()).expect("failed to load gtk");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> *mut c_void> = lib.get(b"gtk_widget_get_visual").expect("failed to load gtk fn");
      func(window)
  }
}

pub fn gdk_x11_visual_get_xvisual(visual: *mut c_void) -> *mut Visual {
  let lib = lib::Library::new(gtk_lib()).expect("failed to load gtk");
  unsafe {
      let func: lib::Symbol<unsafe extern fn(*mut c_void) -> *mut Visual> = lib.get(b"gdk_x11_visual_get_xvisual").expect("failed to load gtk fn");
      func(visual)
  }
}

#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(C)]
pub struct Visual {
  pub ext_data: *mut c_void,
  pub visualid: XID,
  pub class: c_int,
  pub red_mask: c_ulong,
  pub green_mask: c_ulong,
  pub blue_mask: c_ulong,
  pub bits_per_rgb: c_int,
  pub map_entries: c_int,
}