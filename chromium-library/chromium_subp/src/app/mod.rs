use cef;
use utils;
use socket;
// use super::Step;
// use Msg;
use std::os::raw::{c_int};
use std::{mem, slice};
use std::path::PathBuf;
use std::sync::mpsc;
use std::fs::{File, OpenOptions};
use std::io::Write;

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

fn register_function(id: c_int, name: *mut cef::cef_string_t, global: *mut cef::cef_v8value_t, handler: &mut V8Handler) {
    // Add the "myfunc" function to the "window" object.
    let handler_name = utils::cef_string(&format!("{}", id));
    let func = unsafe { cef::cef_v8value_create_function(&handler_name, handler.as_ptr()) };
    let s = unsafe { (*global).set_value_bykey.unwrap()(global, name, func, cef::cef_v8_propertyattribute_t::V8_PROPERTY_ATTRIBUTE_NONE) };
    assert_eq!(s, 1);
}

struct RenderProcessHandler {
    cef: cef::_cef_render_process_handler_t,
    function_handler: Option<V8Handler>,
    // function: *mut cef::cef_v8value_t,
    context: *mut cef::cef_v8context_t,
    pending_functions: Vec<(c_int, cef::cef_string_userfree_t)>,
}

impl RenderProcessHandler {
    fn new() -> RenderProcessHandler {
        RenderProcessHandler {
            cef: RenderProcessHandler::cef_render_process_handler(),
            function_handler: Option::None,
            // function: ::std::ptr::null_mut(),
            context: ::std::ptr::null_mut(),
            pending_functions: Vec::new(),
        }
    }

    pub fn as_ptr(&mut self) -> &mut cef::_cef_render_process_handler_t {
        &mut self.cef
    }

    fn cef_render_process_handler() -> cef::_cef_render_process_handler_t {
        unsafe extern "C" fn on_context_created(
            self_: *mut cef::_cef_render_process_handler_t,
            browser: *mut cef::_cef_browser_t,
            frame: *mut cef::_cef_frame_t,
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

            let pendings = &mut (*rph).pending_functions;
            for pending in pendings.iter() {
                let (id, name) = pending;
                register_function(*id, *name, global, handler);
            }
            pendings.clear();
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
                    println!("RECEIVED EVAL MSG");
                    let id = 1;

                    let eval_ret = utils::cef_string("evalRet");
                    let msg = cef::cef_process_message_create(&eval_ret);
                    let args = (*msg).get_argument_list.unwrap()(msg);
                    let s = (*args).set_int.unwrap()(args, 0, id);
                    assert_eq!(s, 1);
                    let s = (*args).set_bool.unwrap()(args, 1, 1);
                    assert_eq!(s, 1);
                    // let send_fn = (*browser).send_process_message.expect("null send_process_message");
                    // let sent = send_fn(browser, cef::cef_process_id_t::PID_BROWSER, msg);
                    // assert_eq!(sent, 1);
                    socket::socket_client()
                }
                else if cef::cef_string_utf16_cmp(&utils::cef_string("function"), name) == 0 {
                    println!("RECEIVED FUNCTION MSG {:?} {} {}", source_process, cef::cef_currently_on(cef::cef_thread_id_t::TID_IO), cef::cef_currently_on(cef::cef_thread_id_t::TID_RENDERER));

                    let args = (*message).get_argument_list.unwrap()(message);
                    let id = (*args).get_int.unwrap()(args, 0);
                    let name = (*args).get_string.unwrap()(args, 1);

                    let context = (*rph).context;
                    if (*rph).context.is_null() {
                        (*rph).pending_functions.push((id, name));
                    } else {
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
                else if cef::cef_string_utf16_cmp(&utils::cef_string("function_return"), name) == 0 {
                    println!("RECEIVED FUNCTION RETURN {:?} {} {}", source_process, cef::cef_currently_on(cef::cef_thread_id_t::TID_IO), cef::cef_currently_on(cef::cef_thread_id_t::TID_RENDERER));
                    let args = (*message).get_argument_list.unwrap()(message);
                    let id = (*args).get_int.unwrap()(args, 0);
                    let ret = utils::cef_string("THE RET");
                    let handler: Option<&mut V8Handler> = (*rph).function_handler.as_mut();
                    let handler: &mut V8Handler = handler.expect("no handler");
                    handler.returned = true;
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

struct V8Handler {
    cef: cef::_cef_v8handler_t,
    browser: *mut cef::_cef_browser_t,
    returned: bool
}

impl V8Handler {
    fn new(browser: *mut cef::_cef_browser_t) -> V8Handler {
        V8Handler {
            cef: V8Handler::cef_function_handler(),
            browser: browser,
            returned: false
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
            println!("cef_function_handler CALLED");
            let handler = self_ as *mut V8Handler;
            let browser = (*handler).browser;

            let msg_name = utils::cef_string("function");
            let msg = cef::cef_process_message_create(&msg_name);
            let args = (*msg).get_argument_list.unwrap()(msg);
            let s = (*args).set_string.unwrap()(args, 0, name);
            assert_eq!(s, 1);

            let sent = (*browser).send_process_message.unwrap()(browser, cef::cef_process_id_t::PID_BROWSER, msg);
            assert_eq!(sent, 1);

            // wait_return
            println!("before PARK");
            let r = socket::socket_server();
            *retval = cef::cef_v8value_create_bool(1);
            println!("after PARK");
            r
        }
        

        cef::_cef_v8handler_t {
            base: Base::new(mem::size_of::<cef::_cef_v8handler_t>()),
            execute: Option::Some(execute)
        }
    }
}
