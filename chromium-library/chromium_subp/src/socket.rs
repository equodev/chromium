use std::net::{TcpListener, TcpStream};
use std::io::{Read, Write};

unsafe fn any_as_u8_slice<T: Sized>(p: &T) -> &[u8] {
    ::std::slice::from_raw_parts(
        (p as *const T) as *const u8,
        ::std::mem::size_of::<T>(),
    )
}

fn read_struct<T, R: Read>(mut read: R) -> ::std::io::Result<T> {
    let num_bytes = ::std::mem::size_of::<T>();
    unsafe {
        let mut s = ::std::mem::uninitialized();
        let mut buffer = ::std::slice::from_raw_parts_mut(&mut s as *mut T as *mut u8, num_bytes);
        match read.read_exact(buffer) {
            Ok(()) => Ok(s),
            Err(e) => {
                ::std::mem::forget(s);
                Err(e)
            }
        }
    }
}

#[derive(Debug, PartialEq, Eq)]
pub enum MsgType {
    Int,
    Bool,
    Str,
    Array
}

#[derive(Debug, PartialEq, Eq)]
pub struct ReturnMsg {
    kind: MsgType, 
    int_value: u32,
    string_value: Vec<u8>,
    bool_value: bool,
}

#[test]
fn test_serialize() {
    let s = String::from("la la");
    let msg = ReturnMsg { 
        kind: MsgType::Bool,
        int_value: 4,
        bool_value: true,
        string_value: s.into_bytes() 
    };
    let bytes: &[u8] = unsafe { any_as_u8_slice(&msg) };

    println!("bytes: {:?}", bytes);
    
    let mut read_msg = read_struct::<ReturnMsg, _>(bytes).unwrap();
    // read_msg.kind = MsgType::Int;
    // assert_eq!(msg, read_msg); 
}

pub fn socket_server() -> i32 {
    let listener = TcpListener::bind("127.0.0.1:9876").unwrap();
    let r = match listener.accept() {
        Ok((mut socket, addr)) => {
            println!("new client: {:?}", addr);
            
            let mut buffer = vec![0; 10];
            match socket.read_to_end(&mut buffer) {
                Ok(n) => {
                    println!("read from socket: {} {:?}", n, buffer);
                    1
                },
                Err(e) => {
                    println!("couldn't read from socket: {:?}", e);
                    0
                }
            }
        },
        Err(e) => {
            println!("couldn't get client: {:?}", e);
            0
        }
    };
    r
}

pub fn socket_client() -> i32 {
    let mut stream = TcpStream::connect("127.0.0.1:9876").unwrap();        
    // ignore the Result
    match stream.write(&[1]) {
        Ok(n) => {
            println!("write to socket: {}", n);
            1
        },
        Err(e) => {
            println!("couldn't write from socket: {:?}", e);
            0
        }
    }
}
