use cef;
use utils;
use socket;
use std::os::raw::{c_int, c_char};
use std::{mem};
use std::ffi::{CString, CStr};
use std::collections::BTreeMap;
use std::sync::atomic::{AtomicI16, Ordering};

pub struct Base {
}

impl Base {
    pub fn add_ref(base: &mut cef::_cef_base_ref_counted_t) {
        unsafe { base.add_ref.unwrap()(base) };
    }
    
    pub fn release(base: &mut cef::_cef_base_ref_counted_t) {
        unsafe { base.release.unwrap()(base) };
    }
}

pub struct Wrapper<T> {
    pub cef: T,
    count: AtomicI16,
    log: bool
}

impl<T> Wrapper<T> {
    fn new(cef: T, log: bool) -> Wrapper<T> {
        if log {
            //println!("{}:Wrapper new {}", std::process::id(), std::any::type_name::<T>());
        }
        Wrapper::<T> {
            cef,
            count: AtomicI16::new(0),
            log: false
        }
    }

    fn refs(&self) -> &AtomicI16 {
        &self.count
    }
    
    fn incr_ref(&mut self) {
        self.refs().fetch_add(1, Ordering::Relaxed);
        if self.log {
            //println!("{}:incr_ref {}: {}", std::process::id(), std::any::type_name::<T>(), self.refs().load(Ordering::Relaxed));
        }
    }

    fn decr_ref(&mut self) -> c_int {
        if self.refs().fetch_sub(1, Ordering::Relaxed) == 1 {
            if self.log {
                //println!("{}:decr_ref {} Release: {}", std::process::id(), std::any::type_name::<T>(), self.refs().load(Ordering::Relaxed));
            }
            1
        } else {
            if self.log {
                //println!("{}:decr_ref {}: {}", std::process::id(), std::any::type_name::<T>(), self.refs().load(Ordering::Relaxed));
            }
            0
        }
    }

    fn one_ref(&self) -> c_int {
        if self.log {
            //println!("{}:one_ref {}: {}", std::process::id(), std::any::type_name::<T>(), self.refs().load(Ordering::Relaxed));
        }
        if self.refs().load(Ordering::Relaxed) == 1 {
            return 1
        }
        0
    }

    fn atleast_one_ref(&self) -> c_int {
        if self.log {
            //println!("{}:atleast_one_ref {}: {}", std::process::id(), std::any::type_name::<T>(), self.refs().load(Ordering::Relaxed));
        }
        if self.refs().load(Ordering::Relaxed) >= 1 {
            return 1
        }
        0
    }
}

pub trait RefCount<T> {
    fn wrap(cef: T, log: bool) -> Wrapper<T> {
        Wrapper::<T>::new(cef, log)
    }

    fn wrapper(&mut self) -> &mut Wrapper<T>;

    fn cef_base() -> cef::_cef_base_ref_counted_t {
        cef::_cef_base_ref_counted_t {
            size: mem::size_of::<T>(),
            add_ref: Some(Self::base_add_ref),
            has_one_ref: Some(Self::has_one_ref),
            release: Some(Self::rm_ref),
            has_at_least_one_ref: Some(Self::has_at_least_one_ref)
        }
    }
    
    fn as_ptr(&mut self) -> &mut T {
        &mut self.wrapper().cef
    }

    fn from_ptr<'a >(ptr: *mut cef::_cef_base_ref_counted_t) -> &'a mut Wrapper<T> {
        let base_w = unsafe {&mut *(ptr as *mut Wrapper<T>)};
        base_w
    }

    ///
    /// Called to increment the reference count for the object. Should be called
    /// for every new copy of a pointer to a given object.
    ///
    unsafe extern "system" fn base_add_ref(self_: *mut cef::_cef_base_ref_counted_t) {
        let base_w = Self::from_ptr(self_);
        base_w.incr_ref();
    }

    ///
    /// Called to decrement the reference count for the object. If the reference
    /// count falls to 0 the object should self-delete. Returns true (1) if the
    /// resulting reference count is 0.
    ///
    unsafe extern "system" fn rm_ref(self_: *mut cef::_cef_base_ref_counted_t) -> ::std::os::raw::c_int {
        let base_w = Self::from_ptr(self_);
        base_w.decr_ref()
    }
    ///
    /// Returns true (1) if the current reference count is 1.
    ///
    unsafe extern "system" fn has_one_ref(self_: *mut cef::_cef_base_ref_counted_t) -> ::std::os::raw::c_int {
        let base_w = Self::from_ptr(self_);
        base_w.one_ref()
    }

    ///
    /// Returns true (1) if the current reference count is at least 1.
    ///
    unsafe extern "system" fn has_at_least_one_ref(self_: *mut cef::_cef_base_ref_counted_t) -> ::std::os::raw::c_int {
        let base_w = Self::from_ptr(self_);
        base_w.atleast_one_ref()
    }
}

pub struct App {
    wrapper: Wrapper<cef::_cef_app_t>,
    render_process_handler: RenderProcessHandler
}

impl RefCount<cef::_cef_app_t> for App {
    fn wrapper(&mut self) -> &mut Wrapper<cef::_cef_app_t> {
        &mut self.wrapper
    }
}

impl App {
    pub fn new() -> App {
        App {
            wrapper: Self::wrap(Self::cef_app(), false),
            render_process_handler: RenderProcessHandler::new()
        }
    }

    fn cef_app() -> cef::cef_app_t {
        unsafe extern "system" fn get_render_process_handler(self_: *mut cef::_cef_app_t)
                -> *mut cef::_cef_render_process_handler_t {
            let a = self_ as *mut App;
            Base::add_ref(&mut (*a).render_process_handler.wrapper.cef.base);
            (*a).render_process_handler.as_ptr()
        }

        cef::cef_app_t {
            base: Self::cef_base(),
            on_before_command_line_processing: Option::None,
            on_register_custom_schemes: Option::None,
            get_resource_bundle_handler: Option::None,
            get_browser_process_handler: Option::None,
            get_render_process_handler:  Option::Some(get_render_process_handler)
        }
    }
}

#[derive(Clone, Copy)]
struct Browser(*mut cef::_cef_browser_t);
unsafe impl Send for Browser {}

struct RenderProcessHandler {
    wrapper: Wrapper<cef::_cef_render_process_handler_t>,
    browsers: BTreeMap<c_int, BrowserFunctions>,
}

struct BrowserFunctions {
    functions: BTreeMap<c_int, cef::cef_string_userfree_t>,
    v8_handler: Option<V8Handler>,
}

impl RefCount<cef::_cef_render_process_handler_t> for RenderProcessHandler {
    fn wrapper(&mut self) -> &mut Wrapper<cef::_cef_render_process_handler_t> {
        &mut self.wrapper
    }
}

// impl Drop for cef::cef_string_t {
//     fn drop(&mut self) {
//         if self.str.is_null() {
//             println!("DROP _cef_string_t empty {:?}", self.str);
//         } else {
//             println!("DROP _cef_string_t len {} {:?} {:?}", self.length, self.str, utils::str_from_c(utils::cstr_from_cef(self)));
//             unsafe {cef::cef_string_utf16_clear(self) };
//             //let _b = unsafe {Box::from_raw(self.str)}; // consume the pointer and drop it
//         }
//     }
// }

impl RenderProcessHandler {
    fn new() -> Self {
        RenderProcessHandler {
            wrapper: Self::wrap(Self::cef_render_process_handler(), false),
            browsers: BTreeMap::new(),
        }
    }

    fn from<'a>(ptr: *mut cef::_cef_render_process_handler_t) -> &'a mut RenderProcessHandler {
        unsafe { &mut *(ptr as *mut _) }
    }

    fn cef_render_process_handler() -> cef::_cef_render_process_handler_t {
        unsafe extern "system" fn on_context_created(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            frame: *mut cef::_cef_frame_t,
            context: *mut cef::_cef_v8context_t,
        ) {
            if (*frame).is_main.unwrap()(frame) == 1 {
                let rph = RenderProcessHandler::from(self_);
                let browserid = (*browser).get_identifier.unwrap()(browser);
                match rph.browsers.get_mut(&browserid) {
                    Some(fns) => {
                        //println!("{}: new main context in same browser, reregister functions or pendings bid:{}", std::process::id(), browserid);
                        fns.v8_handler = Some(V8Handler::new(frame));
                        if let Some(handler) = &mut fns.v8_handler {
                            let s = (*context).enter.unwrap()(context);
                            assert_eq!(s, 1);
                            let global = (*context).get_global.unwrap()(context);
                            
                            let functions = &mut fns.functions;
                            for (id, name) in functions.iter() {
                                register_function(*id, *name, global, handler);
                            }
                            let s = (*context).exit.unwrap()(context);
                            assert_eq!(s, 1);
                            Base::release(&mut (*global).base);
                        }
                    },
                    None => {
                        //println!("{}: new main context, register new context bid:{}", std::process::id(), browserid);
                        rph.browsers.insert(browserid, BrowserFunctions {
                            functions: BTreeMap::new(),
                            v8_handler: Some(V8Handler::new(frame)),
                        });
                    }
                }
            }
            Base::release(&mut (*browser).base);
            Base::release(&mut (*frame).base);
            Base::release(&mut (*context).base);
        }

        unsafe extern "system" fn on_context_released(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            frame: *mut cef::_cef_frame_t,
            context: *mut cef::_cef_v8context_t,
        ) {
            if (*frame).is_main.unwrap()(frame) == 1 {
                let rph = RenderProcessHandler::from(self_);
                let browserid = (*browser).get_identifier.unwrap()(browser);
                if let Some(fns) = rph.browsers.get_mut(&browserid) {
                    //println!("{}: on_context_released, deregister functions bid:{}", std::process::id(), browserid);
                    if let Some(handler) = &mut fns.v8_handler {
                        // let functions = &mut fns.functions;
                        // for (_id, _name) in functions.iter() {
                        //     Base::release(&mut handler.wrapper().cef.base);
                        // }
                        Base::release(&mut (*handler.frame).base);
                        Base::release(&mut handler.wrapper.cef.base);
                        fns.v8_handler = None;
                    }
                }
            }
            Base::release(&mut (*browser).base);
            Base::release(&mut (*frame).base);
            Base::release(&mut (*context).base);
        }

        unsafe extern "system" fn on_browser_destroyed(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
        ) {
            let rph = RenderProcessHandler::from(self_);
            let browserid = (*browser).get_identifier.unwrap()(browser);
            match rph.browsers.get_mut(&browserid) {
                Some(fns) => {
                    //println!("{}: on_browser_destroyed, release frame {:?}", std::process::id(), browserid);
                    let functions = &mut fns.functions;
                    for (_id, name) in functions.iter() {
                        cef::cef_string_userfree_utf16_free(*name);
                    }
                    functions.clear();
                    rph.browsers.remove(&browserid);
                },
                None => {}
            }
            Base::release(&mut (*browser).base);
        }

        unsafe extern "system" fn on_process_message_received(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            frame: *mut cef::_cef_frame_t,
            source_process: cef::cef_process_id_t,
            message: *mut cef::_cef_process_message_t,
        ) -> c_int {
            let handled = if source_process == cef::cef_process_id_t::PID_BROWSER {
                let valid = (*message).is_valid.unwrap()(message);
                if valid == 0 {
                    return 0;
                }
                let name = (*message).get_name.unwrap()(message);
                let rph = RenderProcessHandler::from(self_);
                let mut eval_cef = utils::cef_string("eval");
                let mut function_cef = utils::cef_string("function");
                let handled = if cef::cef_string_utf16_cmp(&eval_cef, name) == 0 {
                    handle_eval_msg(browser, message);
                    1
                }
                else if cef::cef_string_utf16_cmp(&function_cef, name) == 0 {
                    let args = (*message).get_argument_list.unwrap()(message);
                    let id = (*args).get_int.unwrap()(args, 0);
                    let name = (*args).get_string.unwrap()(args, 1);
                    Base::release(&mut (*args).base);
                    
                    let browserid = (*browser).get_identifier.unwrap()(browser);
                    //println!("{}: RECEIVED FUNCTION MSG for id {}: {:?} {} {}", std::process::id(), browserid, source_process, cef::cef_currently_on(cef::cef_thread_id_t::TID_IO), cef::cef_currently_on(cef::cef_thread_id_t::TID_RENDERER));
                    if !rph.browsers.contains_key(&browserid) {
                        //println!("{}: received function msg but context is not created bid:{}", std::process::id(), browserid);
                        rph.browsers.insert(browserid, BrowserFunctions {
                            functions: BTreeMap::new(),
                            v8_handler: None,
                        });
                    }
                    if let Some(fns) = rph.browsers.get_mut(&browserid) {
                        match fns.functions.insert(id, name) {
                            Some(old) => cef::cef_string_userfree_utf16_free(old),
                            None => {}
                        }
                        if let Some(handler) = &mut fns.v8_handler {
                            let frame = (*browser).get_main_frame.unwrap()(browser);
                            let context = (*frame).get_v8context.unwrap()(frame);
                            let s = (*context).enter.unwrap()(context);
                            assert_eq!(s, 1);
                            let global = (*context).get_global.unwrap()(context);
                            
                            register_function(id, name, global, handler);
                            let s = (*context).exit.unwrap()(context);
                            assert_eq!(s, 1);
                            Base::release(&mut (*global).base);
                            Base::release(&mut (*context).base);
                            Base::release(&mut (*frame).base);
                        }
                    }
                    1
                }
                else { 0 };
                cef::cef_string_utf16_clear(&mut eval_cef);
                cef::cef_string_utf16_clear(&mut function_cef);
                cef::cef_string_userfree_utf16_free(name);
                handled
            } else { 0 };
            Base::release(&mut (*browser).base);
            Base::release(&mut (*frame).base);
            Base::release(&mut (*message).base);
            handled
        }

        cef::_cef_render_process_handler_t {
            base: Self::cef_base(),
            on_render_thread_created: Option::None,
            on_web_kit_initialized: Option::None,
            on_browser_created: Option::None,
            on_browser_destroyed: Option::Some(on_browser_destroyed),
            get_load_handler: Option::None,
            on_context_created: Option::Some(on_context_created),
            on_context_released: Option::Some(on_context_released),
            on_uncaught_exception: Option::None,
            on_focused_node_changed: Option::None,
            on_process_message_received: Option::Some(on_process_message_received),
        }
    }
}

fn register_function(id: c_int, name: *mut cef::cef_string_t, global: *mut cef::cef_v8value_t, handler: &mut V8Handler) {
    // Add the "myfunc" function to the "window" object.
    if unsafe {(*global).has_value_bykey.unwrap()(global, name)} == 1 {
        // println!("register function skipped {}", id);
    } else {
        // println!("register function {}", id);
        let mut handler_name = utils::cef_string(&format!("{}", id));
        let cef_v8handler = handler.as_ptr();
        Base::add_ref(&mut (*cef_v8handler).base);
        unsafe {
            let func = cef::cef_v8value_create_function(&handler_name, cef_v8handler);
            Base::add_ref(&mut (*func).base);
            let s = (*global).set_value_bykey.unwrap()(global, name, func, cef::cef_v8_propertyattribute_t::V8_PROPERTY_ATTRIBUTE_NONE);
            Base::release(&mut (*func).base);
            // Base::release(&mut (*cef_v8handler).base);
            cef::cef_string_utf16_clear(&mut handler_name);
            assert_eq!(s, 1);
        }
    }
}

unsafe fn handle_eval_msg(browser: *mut cef::_cef_browser_t, message: *mut cef::_cef_process_message_t) {
    //println!("RECEIVED EVAL MSG");
    let args = (*message).get_argument_list.unwrap()(message);
    let port = (*args).get_int.unwrap()(args, 0) as u16;
    let _id = (*args).get_int.unwrap()(args, 1);
    let code = (*args).get_string.unwrap()(args, 2);
    handle_eval(browser, code, port);
    cef::cef_string_userfree_utf16_free(code);
    Base::release(&mut (*args).base);
}

unsafe fn handle_eval(browser: *mut cef::_cef_browser_t, code: *const cef::cef_string_t, answer_port: u16) {
    //println!("EVALUAING");
    let frame = (*browser).get_main_frame.unwrap()(browser);
    let context = (*frame).get_v8context.unwrap()(frame);
    let mut url_cef = utils::cef_string("http://text/");
    let mut ret = ::std::ptr::null_mut();
    let mut ex = ::std::ptr::null_mut();

    let s = (*context).eval.unwrap()(context, code, &url_cef, 1, &mut ret, &mut ex);
    if s == 0 {
        //println!("Eval errored");

        let ret_str_cef = (*ex).get_message.unwrap()(ex);
        // let ret_str = utils::cstr_from_cef(ret_str_cef);
        let ret_str_utf8 = cef_to_utf8(ret_str_cef);
        let ret_str = CStr::from_ptr((*ret_str_utf8).str);
        socket::socket_client(answer_port, ret_str.to_owned(), socket::ReturnType::Error);
        cef::cef_string_userfree_utf16_free(ret_str_cef);
        cef::cef_string_userfree_utf8_free(ret_str_utf8);
    } else {
        //println!("Eval succeded {:?}", ret);

        let (ret_str, kind) = convert_type(ret, context);
        socket::socket_client(answer_port, ret_str, kind);
    }
    cef::cef_string_utf16_clear(&mut url_cef);
    Base::release(&mut (*context).base);
    Base::release(&mut (*frame).base);
}

unsafe fn convert_type(ret: *mut cef::cef_v8value_t, context: *mut cef::cef_v8context_t) -> (CString, socket::ReturnType) {
    if (*ret).is_null.expect("is_null")(ret) == 1 || (*ret).is_undefined.unwrap()(ret) == 1 {
        let ret_str = CString::new("").unwrap();
        (ret_str, socket::ReturnType::Null)
    }
    else if (*ret).is_bool.unwrap()(ret) == 1 {
        let ret_cef = (*ret).get_bool_value.unwrap()(ret);
        let ret_str = CString::new(format!("{}", ret_cef)).unwrap();
        (ret_str, socket::ReturnType::Bool)
    }
    else if (*ret).is_int.unwrap()(ret) == 1 {
        let ret_cef = (*ret).get_int_value.unwrap()(ret);
        let ret_str = CString::new(format!("{}", ret_cef)).unwrap();
        (ret_str, socket::ReturnType::Double)
    }
    else if (*ret).is_uint.unwrap()(ret) == 1 {
        let ret_cef = (*ret).get_uint_value.unwrap()(ret);
        let ret_str = CString::new(format!("{}", ret_cef)).unwrap();
        (ret_str, socket::ReturnType::Double)
    }
    else if (*ret).is_double.unwrap()(ret) == 1 {
        let ret_cef = (*ret).get_double_value.unwrap()(ret);
        let ret_str = CString::new(format!("{}", ret_cef)).unwrap();
        (ret_str, socket::ReturnType::Double)
    }
    else if (*ret).is_string.unwrap()(ret) == 1 {
        let ret_str_cef = (*ret).get_string_value.unwrap()(ret);
        // let ret_str = utils::cstr_from_cef(ret_str_cef);
        let ret_str_utf8 = cef_to_utf8(ret_str_cef);
        //let ret_str = CStr::from_ptr(ret_str);
        //(ret_str.to_owned(), socket::ReturnType::Str)
        // let ret_str = CString::from_raw(ret_str);
        // (ret_str, socket::ReturnType::Str)
        // let cstr = utils::str_from_c(ret_str);
        //let cstr = utils::str_from_c((*ret_str_utf8).str);
        if !ret_str_cef.is_null() {
            cef::cef_string_userfree_utf16_free(ret_str_cef);
        }
        // let cstring = CString::new(cstr).expect("Failed to convert v8string to CString");
        let cstring = if !(*ret_str_utf8).str.is_null() {
            let cstr = CStr::from_ptr((*ret_str_utf8).str);
            cstr.to_owned()
        } else { CString::new("").unwrap() };
        cef::cef_string_userfree_utf8_free(ret_str_utf8);
        (cstring, socket::ReturnType::Str)
    }
    else if (*ret).is_array.unwrap()(ret) == 1 {
        //let length = (*ret).get_array_length.unwrap()(ret); // bug on cef, always 0
        let mut arraystr = String::new();
        let array_val = ret;
        
        if !context.is_null() {
            let s = (*context).enter.unwrap()(context);
            assert_eq!(s, 1);
        }

        arraystr.push('"');
        for i in 0..9999 {
            let vali = (*array_val).get_value_byindex.unwrap()(array_val, i);
            if vali.is_null() || (*vali).is_undefined.unwrap()(vali) == 1 {
                break; // end of array
            }
            let (valcstr, valtyp) = convert_type(vali, context);
            let valstr = format!("'{},{}'", valtyp as u32, valcstr.into_string().unwrap());
            if i > 0 {
                arraystr.push_str(";");
            }
            arraystr.push_str(&valstr);
            // println!("array: {}", arraystr);
        }
        arraystr.push('"');
        if !context.is_null() {
            let s = (*context).exit.unwrap()(context);
            assert_eq!(s, 1);
        }
        (CString::new(arraystr).unwrap(), socket::ReturnType::Array)
    }
    else {
        let ret_str = CString::new("51").unwrap();
        (ret_str, socket::ReturnType::Error)
    }
}

struct V8Handler {
    wrapper: Wrapper<cef::_cef_v8handler_t>,
    frame: *mut cef::_cef_frame_t
}

impl RefCount<cef::_cef_v8handler_t> for V8Handler {
    fn wrapper(&mut self) -> &mut Wrapper<cef::_cef_v8handler_t> {
        &mut self.wrapper
    }
}

impl V8Handler {
    fn new(frame: *mut cef::_cef_frame_t) -> V8Handler {
        let mut v8 = V8Handler {
            wrapper: Self::wrap(Self::cef_function_handler(), false),
            frame: frame
        };
        unsafe {Base::add_ref(&mut (*frame).base)};
        Base::add_ref(&mut v8.wrapper.cef.base);
        v8
    }
    
    fn from<'a>(ptr: *mut cef::_cef_v8handler_t) -> &'a mut V8Handler {
        unsafe { &mut *(ptr as *mut _) }
    }

    fn cef_function_handler() -> cef::_cef_v8handler_t {
        unsafe extern "system" fn execute(
            self_: *mut cef::_cef_v8handler_t,
            name: *const cef::cef_string_t,
            object: *mut cef::_cef_v8value_t,
            arguments_count: usize,
            arguments: *const *mut cef::_cef_v8value_t,
            retval: *mut *mut cef::_cef_v8value_t,
            exception: *mut cef::cef_string_t,
        ) -> c_int {
            let handler = V8Handler::from(self_);
            let frame = handler.frame;
            
            let mut msg_name = utils::cef_string("function_call");
            let msg = cef::cef_process_message_create(&msg_name);
            let args = (*msg).get_argument_list.unwrap()(msg);
            // let nm = utils::str_from_c(utils::cstr_from_cef(name));
            let name_uft8 = cef_to_utf8(name);
            let nm = utils::str_from_c((*name_uft8).str);
            let s = (*args).set_int.unwrap()(args, 1, nm.parse::<i32>().expect("failed to parse i32"));
            cef::cef_string_userfree_utf8_free(name_uft8);
            assert_eq!(s, 1);

            for i in 0..arguments_count {
                let v8val = arguments.wrapping_add(i);

                if !v8val.is_null() {
                    let ptr = v8val.read() as *mut cef::_cef_v8value_t;
                    let (cstr, kind) = convert_type(ptr, ::std::ptr::null_mut());
                    let s = (*args).set_int.unwrap()(args, 1+i*2+1, kind as i32);
                    assert_eq!(s, 1);
                    let rstr = cstr.into_string().expect("failed to convert string");
                    let mut strval = utils::cef_string(&rstr);
                    let s = (*args).set_string.unwrap()(args, 1+i*2+2, &strval);
                    cef::cef_string_utf16_clear(&mut strval);
                    assert_eq!(s, 1);
                    Base::release(&mut (*ptr).base);
                } else {
                    //println!("ARG {} is NULL", i);
                }
            }

            //println!("CALLING JAVA FN, WAITING RESPONSE IN SUBP TID_RENDERER:{}", cef::cef_currently_on(cef::cef_thread_id_t::TID_RENDERER));
            
            let cb = |_work: c_int, kind: c_int, value: *const c_char| {
                if socket::ReturnType::from(kind) == socket::ReturnType::Eval {
                    let browser = (*frame).get_browser.unwrap()(frame);
                    let cstr = utils::str_from_c(value);
                    //println!("received eval: {:?}", cstr);
                    let parts: Vec<&str> = cstr.splitn(2, ',').collect();
                    let answer_port = parts[0].parse::<u16>().unwrap();
                    let mut code = utils::cef_string(parts[1]);
                    handle_eval(browser, &code, answer_port);
                    cef::cef_string_utf16_clear(&mut code);
                    Base::release(&mut (*browser).base);
                } 
            };
            let result = socket::wait_response(frame, msg, 0, "", cef::cef_process_id_t::PID_BROWSER, cb);
            match result {
                Ok(return_st) => {
                    match map_type(return_st.kind, return_st.str_value.to_str().unwrap()) {
                        Ok(v) => {
                            *retval = v;
                        },
                        Err(e) => {
                            *exception = utils::cef_string(&e);
                        }
                    }
                },
                Err(_e) => {
                    //println!("socket server error {:?}", e);
                    *exception = utils::cef_string("socket server panic");
                }
            };
            cef::cef_string_utf16_clear(&mut msg_name);
            Base::release(&mut (*args).base);
            Base::release(&mut (*msg).base);
            Base::release(&mut (*object).base);
            1
        }

        cef::_cef_v8handler_t {
            base: Self::cef_base(),
            execute: Option::Some(execute)
        }
    }
}

unsafe fn map_type(kind: socket::ReturnType, str_value: &str) -> Result<*mut cef::cef_v8value_t, &str> {
    match kind {
        socket::ReturnType::Null => {
            Ok(cef::cef_v8value_create_null())
        },
        socket::ReturnType::Bool => {
            let boolean = str_value.parse::<i32>().expect("cannot parse i32");
            Ok(cef::cef_v8value_create_bool(boolean))
        },
        socket::ReturnType::Double => {
            let double = str_value.parse::<f64>().expect("cannot parse f64");
            Ok(cef::cef_v8value_create_double(double))
        },
        socket::ReturnType::Str => {
            let str_cef = utils::cef_string(str_value);
            Ok(cef::cef_v8value_create_string(&str_cef))
        },
        socket::ReturnType::Array => {
            let rstr = str_value;
            let rstr = rstr.get(1..rstr.len()-1).expect("not quoted");
            let v = split(rstr, '"', ';');
            let array = cef::cef_v8value_create_array(v.len() as i32);
            for i in 0..v.len() {
                let elem_unquoted = v[i].get(1..v[i].len()-1).expect("elem not quoted");
                let parts = splitn(elem_unquoted, 2, '\'', ',');
                let elem_type = socket::ReturnType::from(parts[0].parse::<i32>().unwrap());
                let elem_value = map_type(elem_type, parts[1]);
                let s = (*array).set_value_byindex.unwrap()(array, i as i32, elem_value.expect("invalid elem type"));
                assert_eq!(s, 1, "failed to set v8array index");
            }
            Ok(array)
        },
        _ => {
            //println!("unsupported {:?}", kind);
            Err(str_value)
        }
    }
}

fn split<'a>(rstr: &'a str, quote: char, sep: char) -> Vec<&'a str> {
    let mut in_string = false;
    let v: Vec<&str> = rstr.split_terminator(|c| {
        if c == quote && in_string {
            in_string = false;
        } else if c == quote && !in_string {
            in_string = true;
        } 
        c == sep && !in_string
    }).collect();
    v
}

fn splitn<'a>(rstr: &'a str, max: usize, quote: char, sep: char) -> Vec<&'a str> {
    let mut in_string = false;
    let v: Vec<&str> = rstr.splitn(max, |c| {
        if c == quote && in_string {
            in_string = false;
        } else if c == quote && !in_string {
            in_string = true;
        } 
        c == sep && !in_string
    }).collect();
    v
}

// callers should free string
fn cef_to_utf8(cefstring: *const cef::cef_string_t) -> *mut cef::cef_string_utf8_t {
    let utf8 = unsafe { cef::cef_string_userfree_utf8_alloc() };
    if !cefstring.is_null() {
        unsafe { cef::cef_string_utf16_to_utf8((*cefstring).str, (*cefstring).length, utf8) };
    }
    utf8
}