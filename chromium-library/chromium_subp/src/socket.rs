use std::net::{TcpListener, TcpStream};
use std::io::{Read, Write};
use std::ffi::{CString, CStr};
use std::thread::{self, JoinHandle};

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

pub fn read_response() -> (u16, JoinHandle<ReturnSt>) {
    let port = get_available_port().expect("no ports available");
    let child = thread::spawn(move || {
        let listener = TcpListener::bind(("127.0.0.1", port)).unwrap();
        match listener.accept() {
            Ok((mut socket, addr)) => {
                println!("new client: {:?}", addr);
                
                let mut buffer = Vec::new();
                match socket.read_to_end(&mut buffer) {
                    Ok(n) => {
                        println!("read from socket: {} {} {:?}", n, ::std::mem::size_of::<ReturnSt>(), buffer);
                        let ret = read_buffer(&buffer);
                        println!("st: {:?}", ret);
                        ret
                    },
                    Err(e) => {
                        println!("couldn't read from socket: {:?}", e);
                        panic!(e)
                    }
                }
            },
            Err(e) => {
                println!("couldn't get client: {:?}", e);
                panic!(e)
            }
        }
    });
    (port, child)
}

pub fn socket_client(port: u16, ret: CString, ret_type: ReturnType) -> i32 {
    let mut stream = TcpStream::connect(("127.0.0.1", port)).unwrap();
    write_buffer(&mut stream, ret, ret_type);
    1
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