extern crate chromium_swt;

use chromium_swt::cef;
use chromium_swt::utils;
use std::os::raw::{c_int};
use std::ptr::null_mut;

fn subp() {
    println!("IN SUBP");
    let main_args = utils::prepare_args();
    println!("Calling cef_execute_process");
    let exit_code: c_int = unsafe { cef::cef_execute_process(&main_args, null_mut(), null_mut()) };
    println!("exiting subp with {}", exit_code);
    std::process::exit(exit_code);
}

fn main() {
    subp();
}
