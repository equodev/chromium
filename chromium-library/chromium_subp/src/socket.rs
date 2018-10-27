use std::net::{TcpListener, TcpStream};
use std::io::{Read, Write};
use std::os::raw::c_char;
use std::ffi::CString;

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
    pub str_value: *mut c_char,
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

    let mut s2 = String::new();
    (&channel[skip .. ]).read_to_string(&mut s2).expect("Failed to read string");
    let cstr = CString::new(s2).unwrap();
    let raw_str = cstr.into_raw();
    ReturnSt {
        kind: read_msg.kind,
        str_value: raw_str,
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
    
    let bytes = val.into_bytes();
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

pub fn socket_server() -> Result<ReturnSt, ::std::io::Error> {
    let listener = TcpListener::bind("127.0.0.1:9687").unwrap();
    match listener.accept() {
        Ok((mut socket, addr)) => {
            println!("new client: {:?}", addr);
            
            let mut buffer = Vec::new();
            match socket.read_to_end(&mut buffer) {
                Ok(n) => {
                    println!("read from socket: {} {} {:?}", n, ::std::mem::size_of::<ReturnSt>(), buffer);
                    let ret = read_buffer(&buffer);
                    Ok(ret)
                },
                Err(e) => {
                    println!("couldn't read from socket: {:?}", e);
                    Err(e)
                }
            }
        },
        Err(e) => {
            println!("couldn't get client: {:?}", e);
            Err(e)
        }
    }
}

pub fn socket_client(ret: CString, ret_type: ReturnType) -> i32 {
    let mut stream = TcpStream::connect("127.0.0.1:9687").unwrap();        
    write_buffer(&mut stream, ret, ret_type);
    1
}
