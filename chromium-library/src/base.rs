use cef;

use std::option::Option;

const DEBUG_REFERENCE_COUNTING: bool = true;

pub type CefBase = cef::cef_base_ref_counted_t;

///
// Structure defining the reference count implementation functions. All
// framework structures must include the cef_base_t structure first.
///
impl CefBase {
    pub fn new(st_size: usize) -> CefBase {
        // let count = 0;
        println!("cef_base_t.size = {}", st_size);

        ///
        // Increment the reference count.
        ///
        pub unsafe extern "C" fn add_ref(_: *mut cef::cef_base_ref_counted_t) {
            debug_callback("cef_base_t.add_ref");
            if DEBUG_REFERENCE_COUNTING {
                // println!("+");
            }
        }

        ///
        // Decrement the reference count.  Delete this object when no references
        // remain.
        ///
        pub unsafe extern "C" fn release(_: *mut cef::cef_base_ref_counted_t)
                                                -> ::std::os::raw::c_int {
            debug_callback("cef_base_t.release");
            if DEBUG_REFERENCE_COUNTING {
                // println!("-");
            }
            1
        }

        ///
        // Returns the current number of references.
        ///
        pub unsafe extern "C" fn has_one_ref(_: *mut cef::cef_base_ref_counted_t)
                                                -> ::std::os::raw::c_int {
            debug_callback("cef_base_t.get_ref");
            if DEBUG_REFERENCE_COUNTING {
                // println!("=");
            }
            1
        }


        let base = CefBase {
            size: st_size,
            add_ref: Option::Some(add_ref),
            release: Option::Some(release),
            has_one_ref: Option::Some(has_one_ref)
        };
        base
    }
}

fn debug_callback(_: &str) {
	//println!("{}", l)
}
