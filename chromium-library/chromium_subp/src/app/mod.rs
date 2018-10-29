use cef;
use utils;
use socket;
// use super::Step;
// use Msg;
use std::os::raw::{c_int};
use std::{mem};
use std::ffi::{CString, CStr};

pub struct Base {
}

impl Base {
    pub fn new(size: usize) -> cef::_cef_base_ref_counted_t {
        cef::_cef_base_ref_counted_t {
            size,
            add_ref: Option::None,
            has_one_ref: Option::None,
            release: Option::None
        }
    }
}

pub struct App {
    cef: cef::_cef_app_t,
    render_process_handler: RenderProcessHandler
}

impl App {
    pub fn new() -> App {
        App {
            cef: App::cef_app(),
            render_process_handler: RenderProcessHandler::new()
        }
    }

    pub fn as_ptr(&mut self) -> &mut cef::cef_app_t {
        &mut self.cef
    }

    fn cef_app() -> cef::cef_app_t {
        unsafe extern "C" fn get_render_process_handler(self_: *mut cef::_cef_app_t)
                -> *mut cef::_cef_render_process_handler_t {
            let a = self_ as *mut App;
            (*a).render_process_handler.as_ptr()
        }

        cef::cef_app_t {
            base: Base::new(mem::size_of::<cef::cef_app_t>()),
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
    cef: cef::_cef_render_process_handler_t,
    function_handler: Option<V8Handler>,
    // function: *mut cef::cef_v8value_t,
    context: *mut cef::cef_v8context_t,
    functions: Vec<(c_int, cef::cef_string_userfree_t)>,
}

impl RenderProcessHandler {
    fn new() -> RenderProcessHandler {
        RenderProcessHandler {
            cef: RenderProcessHandler::cef_render_process_handler(),
            function_handler: Option::None,
            // function: ::std::ptr::null_mut(),
            context: ::std::ptr::null_mut(),
            functions: Vec::new(),
        }
    }

    pub fn as_ptr(&mut self) -> &mut cef::_cef_render_process_handler_t {
        &mut self.cef
    }

    fn cef_render_process_handler() -> cef::_cef_render_process_handler_t {
        unsafe extern "C" fn on_context_created(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            _frame: *mut cef::_cef_frame_t,
            context: *mut cef::_cef_v8context_t,
        ) {
            println!("CONTEXT CREATED");
            let rph = self_ as *mut RenderProcessHandler;
            
            (*rph).function_handler = Option::Some(V8Handler::new(browser));
            let handler: Option<&mut V8Handler> = (*rph).function_handler.as_mut();
            let handler: &mut V8Handler = handler.expect("no handler");
            
            // (*rph).function = func;
            (*rph).context = context;
            
            // Retrieve the context's window object.
            let global = (*context).get_global.unwrap()(context);

            let pendings = &mut (*rph).functions;
            for pending in pendings.iter() {
                let (id, name) = pending;
                register_function(*id, *name, global, handler);
            }
        }

        unsafe extern "C" fn on_process_message_received(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            source_process: cef::cef_process_id_t,
            message: *mut cef::_cef_process_message_t,
        ) -> c_int {
            if source_process == cef::cef_process_id_t::PID_BROWSER {
                let valid = (*message).is_valid.unwrap()(message);
                let name = (*message).get_name.unwrap()(message);
                if valid == 0 {
                    return 0;
                }
                let rph = self_ as *mut RenderProcessHandler;
                let handled = if cef::cef_string_utf16_cmp(&utils::cef_string("eval"), name) == 0 {
                    handle_eval(browser, message);
                    1
                }
                else if cef::cef_string_utf16_cmp(&utils::cef_string("function"), name) == 0 {
                    println!("RECEIVED FUNCTION MSG {:?} {} {}", source_process, cef::cef_currently_on(cef::cef_thread_id_t::TID_IO), cef::cef_currently_on(cef::cef_thread_id_t::TID_RENDERER));

                    let args = (*message).get_argument_list.unwrap()(message);
                    let id = (*args).get_int.unwrap()(args, 0);
                    let name = (*args).get_string.unwrap()(args, 1);

                    let context = (*rph).context;
                    (*rph).functions.push((id, name));
                    if !(*rph).context.is_null() {
                        let handler: Option<&mut V8Handler> = (*rph).function_handler.as_mut();
                        let handler: &mut V8Handler = handler.expect("no handler");
                        // let frame = (*browser).get_main_frame.unwrap()(browser);
                        // let context = (*frame).get_v8context.unwrap()(frame);
                        let s = (*context).enter.unwrap()(context);
                        assert_eq!(s, 1);
                        let global = (*context).get_global.unwrap()(context);

                        register_function(id, name, global, handler);
                        let s = (*context).exit.unwrap()(context);
                        assert_eq!(s, 1);
                    }
                    1
                }
                else {
                    0
                };

                cef::cef_string_userfree_utf16_free(name);
                return handled;
            }
            0
        }

        cef::_cef_render_process_handler_t {
            base: Base::new(mem::size_of::<cef::_cef_render_process_handler_t>()),
            on_render_thread_created: Option::None,
            on_web_kit_initialized: Option::None,
            on_browser_created: Option::None,
            on_browser_destroyed: Option::None,
            get_load_handler: Option::None,
            on_before_navigation: Option::None,
            on_context_created: Option::Some(on_context_created),
            on_context_released: Option::None,
            on_uncaught_exception: Option::None,
            on_focused_node_changed: Option::None,
            on_process_message_received: Option::Some(on_process_message_received),
        }
    }
}

fn register_function(id: c_int, name: *mut cef::cef_string_t, global: *mut cef::cef_v8value_t, handler: &mut V8Handler) {
    // Add the "myfunc" function to the "window" object.
    let handler_name = utils::cef_string(&format!("{}", id));
    let func = unsafe { cef::cef_v8value_create_function(&handler_name, handler.as_ptr()) };
    let s = unsafe { (*global).set_value_bykey.unwrap()(global, name, func, cef::cef_v8_propertyattribute_t::V8_PROPERTY_ATTRIBUTE_NONE) };
    assert_eq!(s, 1);
}

unsafe fn handle_eval(browser: *mut cef::_cef_browser_t, message: *mut cef::_cef_process_message_t) {
    println!("RECEIVED EVAL MSG");
    let args = (*message).get_argument_list.unwrap()(message);
    let port = (*args).get_int.unwrap()(args, 0) as u16;
    // let id = (*args).get_int.unwrap()(args, 1);
    let code = (*args).get_string.unwrap()(args, 2);

    let frame = (*browser).get_main_frame.unwrap()(browser);
    let context = (*frame).get_v8context.unwrap()(frame);
    let url_cef = utils::cef_string("http://text/");
    let mut ret = ::std::ptr::null_mut();
    let mut ex = ::std::ptr::null_mut();

    let s = (*context).eval.unwrap()(context, code, &url_cef, 1, &mut ret, &mut ex);
    if s == 0 {
        println!("Eval errored");

        let ret_str_cef = (*ex).get_message.unwrap()(ex);
        let ret_str = utils::cstr_from_cef(ret_str_cef);
        let ret_str = CStr::from_ptr(ret_str);
        socket::socket_client(port, ret_str.to_owned(), socket::ReturnType::Error);
        cef::cef_string_userfree_utf16_free(ret_str_cef);
    } else {
        println!("Eval succeded {:?}", ret);

        let (ret_str, kind) = convert_type(ret);
        socket::socket_client(port, ret_str, kind);
    }
}

unsafe fn convert_type(ret: *mut cef::cef_v8value_t) -> (CString, socket::ReturnType) {
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
        let ret_str = utils::cstr_from_cef(ret_str_cef);
        //let ret_str = CStr::from_ptr(ret_str);
        //(ret_str.to_owned(), socket::ReturnType::Str)
        // let ret_str = CString::from_raw(ret_str);
        // (ret_str, socket::ReturnType::Str)
        let cstr = utils::str_from_c(ret_str);
        (CString::new(cstr).expect("Failed to convert v8string to CString"), socket::ReturnType::Str)
    }
    else if (*ret).is_array.unwrap()(ret) == 1 {
        let length = (*ret).get_array_length.unwrap()(ret);
        let mut arraystr = String::new();
        for i in 0..length {
            let vali = (*ret).get_value_byindex.unwrap()(ret, i);
            let valstr = if vali == ::std::ptr::null_mut() {
                // See: https://bitbucket.org/chromiumembedded/cef/issues/1865/cef_v8value_t-get_value_byindex-does
                format!("null")
            }
            else if (*vali).is_null.unwrap()(vali) == 1 || (*vali).is_undefined.unwrap()(vali) == 1 {
                format!("null")
            }
            else if (*vali).is_bool.unwrap()(vali) == 1 {
                let ret_cef = (*vali).get_bool_value.unwrap()(vali);
                if ret_cef == 1 { format!("true") } else { format!("false") }
            }
            else if (*vali).is_int.unwrap()(vali) == 1 {
                let ret_cef = (*vali).get_int_value.unwrap()(vali);
                format!("{}", ret_cef)
            }
            else if (*vali).is_uint.unwrap()(vali) == 1 {
                let ret_cef = (*vali).get_uint_value.unwrap()(vali);
                format!("{}", ret_cef)
            }
            else if (*vali).is_double.unwrap()(vali) == 1 {
                let ret_cef = (*vali).get_double_value.unwrap()(vali);
                format!("{}", ret_cef)
            }
            else if (*vali).is_string.unwrap()(vali) == 1 {
                let ret_str_cef = (*vali).get_string_value.unwrap()(vali);
                let ret_str = utils::cstr_from_cef(ret_str_cef);
                let ret_str = CStr::from_ptr(ret_str);
                let ret_str = ret_str.to_str().expect("failed to convert string array arg");
                format!("\"{}\"", ret_str)
            }
            else {
                format!("error")
            };
            if i > 0 {
                arraystr.push(';');
            }
            arraystr.push_str(&valstr);
            println!("array: {}", arraystr);
        }
        (CString::new(arraystr).unwrap(), socket::ReturnType::Array)
    }
    else {
        let ret_str = CString::new("51").unwrap();
        (ret_str, socket::ReturnType::Error)
    }
}

struct V8Handler {
    cef: cef::_cef_v8handler_t,
    browser: *mut cef::_cef_browser_t
}

impl V8Handler {
    fn new(browser: *mut cef::_cef_browser_t) -> V8Handler {
        V8Handler {
            cef: V8Handler::cef_function_handler(),
            browser: browser
        }
    }

    pub fn as_ptr(&mut self) -> &mut cef::_cef_v8handler_t {
        &mut self.cef
    }

    fn cef_function_handler() -> cef::_cef_v8handler_t {
        unsafe extern "C" fn execute(
            self_: *mut cef::_cef_v8handler_t,
            name: *const cef::cef_string_t,
            _object: *mut cef::_cef_v8value_t,
            arguments_count: usize,
            arguments: *const *const cef::_cef_v8value_t,
            retval: *mut *mut cef::_cef_v8value_t,
            exception: *mut cef::cef_string_t,
        ) -> c_int {
            let handler = self_ as *mut V8Handler;
            let browser = (*handler).browser;

            let msg_name = utils::cef_string("function_call");
            let msg = cef::cef_process_message_create(&msg_name);
            let args = (*msg).get_argument_list.unwrap()(msg);
            let nm = utils::str_from_c(utils::cstr_from_cef(name));
            let s = (*args).set_int.unwrap()(args, 1, nm.parse::<i32>().expect("failed to parse i32"));
            assert_eq!(s, 1);

            for i in 0..arguments_count {
                let v8val = arguments.wrapping_add(i);

                if !v8val.is_null() {
                    let ptr = v8val.read() as *mut cef::_cef_v8value_t;
                    let (cstr, kind) = convert_type(ptr);
                    let s = (*args).set_int.unwrap()(args, 1+i*2+1, kind as i32);
                    assert_eq!(s, 1);
                    let rstr = cstr.into_string().expect("failed to convert string");
                    let strval = utils::cef_string(&rstr);
                    let s = (*args).set_string.unwrap()(args, 1+i*2+2, &strval);
                    assert_eq!(s, 1);
                } else {
                        println!("ARG {} is NULL", i);
                }
            }

            let (port, child) = socket::read_response();
            let s = (*args).set_int.unwrap()(args, 0, port as i32);
            assert_eq!(s, 1);
            let sent = (*browser).send_process_message.unwrap()(browser, cef::cef_process_id_t::PID_BROWSER, msg);
            assert_eq!(sent, 1);

            // wait_return
            let result = child.join();
            match result {
                Ok(return_st) => {
                    match return_st.kind {
                        socket::ReturnType::Null => {
                            *retval = cef::cef_v8value_create_null();
                        },
                        socket::ReturnType::Bool => {
                            let boolean = return_st.str_value.to_str().unwrap().parse::<i32>().expect("cannot parse i32");
                            *retval = cef::cef_v8value_create_bool(boolean);
                        },
                        socket::ReturnType::Double => {
                            let double = return_st.str_value.to_str().unwrap().parse::<f64>().expect("cannot parse f64");
                            *retval = cef::cef_v8value_create_double(double);
                        },
                        socket::ReturnType::Str => {
                            let str_cef = utils::cef_string(return_st.str_value.to_str().unwrap());
                            *retval = cef::cef_v8value_create_string(&str_cef);
                        },
                        socket::ReturnType::Array => {
                            let rstr = return_st.str_value.to_str().unwrap();
                            let mut in_string = false;
                            let v: Vec<&str> = rstr.split(|c| {
                                if c == '"' && in_string {
                                    in_string = false;
                                } else if c == '"' && !in_string {
                                    in_string = true;
                                } 
                                c == ';' && !in_string
                            }).collect();
                            let array = cef::cef_v8value_create_array(v.len() as i32);
                            for i in 0..v.len() {
                                let vi = if v[i].chars().next() == Some('"') {
                                    let strcef = utils::cef_string(v[i].get(1..v[i].len()-1).expect("bad str array"));
                                    cef::cef_v8value_create_string(&strcef)
                                } else if v[i] == "null" {
                                    cef::cef_v8value_create_null()
                                } else if v[i] == "true" {
                                    cef::cef_v8value_create_bool(1)
                                } else if v[i] == "false" {
                                    cef::cef_v8value_create_bool(0)
                                } else if v[i].parse::<u32>().is_ok() {
                                    cef::cef_v8value_create_uint(v[i].parse::<u32>().unwrap())
                                } else if v[i].parse::<i32>().is_ok() {
                                    cef::cef_v8value_create_int(v[i].parse::<i32>().unwrap())
                                } else if v[i].parse::<f64>().is_ok() {
                                    cef::cef_v8value_create_double(v[i].parse::<f64>().unwrap())
                                } else {
                                    cef::cef_v8value_create_null()
                                };
                                let s = (*array).set_value_byindex.unwrap()(array, i as i32, vi);
                                assert_eq!(s, 1, "failed to set v8array index");
                            }
                            *retval = array;
                        },
                        _ => {
                            println!("unsupported {:?}", return_st.kind);
                            *exception = utils::cef_string(return_st.str_value.to_str().unwrap());
                        }
                    }
                },
                Err(_) => {
                    *exception = utils::cef_string("socker server panic");
                }
            } 
            1
        }

        cef::_cef_v8handler_t {
            base: Base::new(mem::size_of::<cef::_cef_v8handler_t>()),
            execute: Option::Some(execute)
        }
    }
}
