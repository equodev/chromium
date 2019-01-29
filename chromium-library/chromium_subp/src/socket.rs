use std::net::{TcpListener, TcpStream};
use std::io::{Read, Write};
use std::ffi::{CString, CStr};
use std::os::raw::{c_char, c_int};
// use std::thread::{self, JoinHandle};
use cef;

#[derive(Debug, PartialEq, Eq)]
pub enum ReturnType {
    Double = 0,
    Bool = 1,
    Str = 2,
    Null = 3,
    Array = 4,
    Error = 5
}

impl ReturnType {
    pub fn from(i: i32) -> ReturnType {
        match i {
            0 => ReturnType::Double,
            1 => ReturnType::Bool,
            2 => ReturnType::Str,
            3 => ReturnType::Null,
            4 => ReturnType::Array,
            _ => ReturnType::Error,
        }
    }
}

#[derive(Debug, PartialEq, Eq)]
pub struct ReturnSt {
    pub kind: ReturnType, 
    pub str_value: CString
}

#[derive(Debug, PartialEq, Eq)]
struct ReturnMsg {
    kind: ReturnType, 
    length: usize,
}

unsafe fn any_as_u8_slice<T: Sized>(p: &T) -> &[u8] {
    ::std::slice::from_raw_parts(
        (p as *const T) as *const u8,
        ::std::mem::size_of::<T>(),
    )
}

fn read_struct<T, R: Read>(mut read: R) -> ::std::io::Result<(usize,T)> {
    let num_bytes = ::std::mem::size_of::<T>();
    unsafe {
        let mut s = ::std::mem::uninitialized();
        let buffer = ::std::slice::from_raw_parts_mut(&mut s as *mut T as *mut u8, num_bytes);
        match read.read_exact(buffer) {
            Ok(()) => Ok((num_bytes, s)),
            Err(e) => {
                ::std::mem::forget(s);
                Err(e)
            }
        }
    }
}

fn read_buffer(channel: &[u8]) -> ReturnSt {
    let (skip, read_msg) = read_struct::<ReturnMsg, _>(channel).unwrap();

    let cstr = CStr::from_bytes_with_nul(&channel[skip .. ]).expect("Failed to read string");
    ReturnSt {
        kind: read_msg.kind,
        str_value: cstr.to_owned(),
    }
}

fn write_buffer<W: Write>(channel: &mut W, val: CString, kind: ReturnType) {
   let msg = ReturnMsg { 
        kind,
        length: val.as_bytes().len()
    };
    let bytes: &[u8] = unsafe { any_as_u8_slice(&msg) };
    let mut buffer = Vec::new();
    buffer.write_all(bytes).expect("Failed to write struct");    
    
    let bytes = val.into_bytes_with_nul();
    buffer.write_all(&bytes).expect("Failed to write return");
    channel.write_all(&buffer).expect("Failed to write buffer");
}

#[test]
fn serialize_string() {
    let mut channel: Vec<u8> = vec!();
    
    let s = CString::new("o la la").unwrap();
    write_buffer(&mut channel, s, ReturnType::Str);

    let read_st = read_buffer(&channel);
    
    assert_eq!(ReturnType::Str, read_st.kind);
    // assert_eq!(7, read_st.length);
    assert_eq!(CString::new("o la la").unwrap(), unsafe{CString::from_raw(read_st.str_value)});
}

#[test]
fn serialize_null() {
    let mut channel: Vec<u8> = vec!();
    
    let s = CString::new("").unwrap();
    write_buffer(&mut channel, s, ReturnType::Null);

    let read_st = read_buffer(&channel);
    
    assert_eq!(ReturnType::Null, read_st.kind);
    // assert_eq!(7, read_st.length);
    assert_eq!(CString::new("").unwrap(), unsafe{CString::from_raw(read_st.str_value)});
}

pub fn wait_response(browser: *mut cef::cef_browser_t, 
        msg: *mut cef::cef_process_message_t,
        args: *mut cef::_cef_list_value_t,
        target: cef::cef_process_id_t,
        callback: Option<unsafe extern "C" fn(work: c_int, kind: ReturnType, value: *const c_char)>
        ) -> Result<ReturnSt, String> {
    match get_available_port() {
        Some(port) => {
            let s = unsafe {(*args).set_int.unwrap()(args, 0, port as i32) };
            assert_eq!(s, 1);

            match TcpListener::bind(("127.0.0.1", port)) {
                Ok(listener) => {
                    let sent = unsafe {(*browser).send_process_message.unwrap()(browser, target, msg)};
                    assert_eq!(sent, 1);

                    println!("new server, waiting response in :{:?}", port);
                    let mut res = None;
                    listener.set_nonblocking(true).expect("Cannot set non-blocking");
                    for stream in listener.incoming() {
                        match stream {
                            Ok(mut stream) => {
                                println!("new client!");
                                let mut buffer = Vec::new();
                                loop {
                                    match stream.read_to_end(&mut buffer) {
                                        Ok(n) => {
                                            println!("read from socket: {} {} {:?}", n, ::std::mem::size_of::<ReturnSt>(), buffer);
                                            let ret = read_buffer(&buffer);
                                            println!("st: {:?}", ret);
                                            res = Some(Ok(ret));
                                            break;
                                        },
                                        Err(ref e) if e.kind() == ::std::io::ErrorKind::WouldBlock => {

                                        },
                                        Err(e) => {
                                            println!("couldn't read from socket: {:?}", e);
                                            res = Some(Err(e.to_string()));
                                            break;
                                        }
                                    }
                                }
                            },
                            Err(_e) => { 
                                // println!("couldn't get client: {:?}", e);
                                unsafe {
                                    if let Some(call) = callback {
                                        call(1, ReturnType::Error, ::std::ptr::null());
                                    }
                                };
                                unsafe { cef::cef_do_message_loop_work() };
                            }
                        };
                        if res.is_some() {
                            break;
                        }
                    };
                    res.unwrap()
                },
                Err(e) => {
                    println!("couldn't bind port: {:?}", e);
                    Err(e.to_string())
                }
            }
        },
        None => {
            Err("no ports available".to_string())
        }
    }
}

pub fn socket_client(port: u16, ret: CString, ret_type: ReturnType) -> i32 {
    match TcpStream::connect(("127.0.0.1", port)) {
        Ok(mut stream) => {
            write_buffer(&mut stream, ret, ret_type);
            1
        }
        Err(e) => {
            println!("Cannot connect to renderer socket {:?}", e);
            0
        }
    }
}

fn get_available_port() -> Option<u16> {
    (8001..9999)
        .find(|port| port_is_available(*port))
}

fn port_is_available(port: u16) -> bool {
    match TcpListener::bind(("127.0.0.1", port)) {
        Ok(_) => true,
        Err(_) => false,
    }
}