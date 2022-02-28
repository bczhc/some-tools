use crate::transfer::error::result::Result;
use crate::transfer::lib::{EnumU8Value, Mark, HEADER};
use byteorder::{BigEndian, WriteBytesExt};
use std::io::Write;
use std::net::{SocketAddr, TcpStream};

pub fn send(ip: SocketAddr, msg: &str) -> Result<()> {
    let mut tcp_stream = TcpStream::connect(ip)?;
    tcp_stream.write_all(HEADER)?;
    tcp_stream.write_u8(Mark::Text.u8_value())?;
    tcp_stream.write_u32::<BigEndian>(msg.len() as u32)?;
    tcp_stream.write_all(msg.as_bytes())?;
    Ok(())
}
