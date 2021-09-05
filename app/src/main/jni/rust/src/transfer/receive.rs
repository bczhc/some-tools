use std::io::Read;
use std::net::{Ipv4Addr, SocketAddrV4, TcpListener, TcpStream};
use std::sync::Arc;
use std::thread::spawn;

use byteorder::{BigEndian, ReadBytesExt};

use crate::transfer::error::result::{ContractError, Error, Result};
use crate::transfer::lib::{FromValue, Mark, Status, HEADER};

pub fn async_receive<F>(port: u16, callback: F)
where
    F: Fn(Result<String>) + Send + 'static + Sync,
{
    spawn(move || {
        let listener = TcpListener::bind(SocketAddrV4::new(Ipv4Addr::new(0, 0, 0, 0), port));
        if let Err(e) = listener {
            callback(Err(e.into()));
            return;
        }
        let listener = listener.unwrap();

        let result = listener.accept();
        if let Err(e) = result {
            callback(Err(e.into()));
            return;
        }
        let (mut tcp_stream, _) = result.unwrap();

        let result = read_msg(&mut tcp_stream);
        callback(result);
    });
}

fn read_msg(tcp_stream: &mut TcpStream) -> Result<String> {
    let mut header = [0_u8; 8];
    tcp_stream.read(&mut header)?;
    if &header != HEADER {
        return Err(ContractError::InvalidHeader.into());
    }

    let mark = tcp_stream.read_u8()?;
    let mark = Mark::from_value_resulted(mark)?;

    // TODO: other transfer types supports
    match mark {
        Mark::Text => {}
        _ => {
            return Err(ContractError::UnsupportedType.into());
        }
    }

    let msg_length = tcp_stream.read_u32::<BigEndian>()? as usize;
    let mut msg = vec![0_u8; msg_length];
    tcp_stream.read(&mut msg)?;

    Ok(String::from_utf8(msg)?)
}

trait FromMarkValue {
    fn from_value_resulted(value: u8) -> Result<Mark> {
        let option = Mark::from_value(value);
        match option {
            None => Err(ContractError::InvalidMark(value).into()),
            Some(mark) => Ok(mark),
        }
    }
}

impl FromMarkValue for Mark {}
