use std::net::{Ipv4Addr, SocketAddr, SocketAddrV4, TcpListener, TcpStream};
use std::thread::spawn;

use jni::objects::{JByteBuffer, JClass, JObject, JString, JValue, ReleaseMode};
use jni::sys::{jbyteArray, jshort, jstring};
use jni::JNIEnv;

use crate::jni_helper;
use crate::jni_helper::CheckOrThrow;
use crate::transfer::error::result::*;
use crate::transfer::lib::Status;
use crate::transfer::receive::async_receive;
use crate::transfer::send::send;
use pnet::ipnetwork::IpNetwork;
use std::ops::Deref;
use std::sync::Arc;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Transfer_send(
    env: JNIEnv,
    _class: JClass,
    ip_array: jbyteArray,
    port: jshort,
    msg: JString,
) {
    let result = send_run(env, ip_array, port as u16, msg);
    result.check_or_throw(env).unwrap();
}

fn send_run(env: JNIEnv, ip_array: jbyteArray, port: u16, msg: JString) -> Result<()> {
    let mut ip = [0_i8; 4];
    env.get_byte_array_region(ip_array, 0, &mut ip)?;

    let msg = env.get_string(msg)?;
    let msg = msg.to_str()?;

    let socket_addr_v4 = SocketAddrV4::new(
        Ipv4Addr::new(ip[0] as u8, ip[1] as u8, ip[2] as u8, ip[3] as u8),
        port,
    );
    send(SocketAddr::V4(socket_addr_v4), msg)?;

    Ok(())
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Transfer_startAsyncReceive(
    env: JNIEnv,
    _class: JClass,
    port: jshort,
    callback: JObject,
) {
    let jvm = env.get_java_vm().unwrap();
    let jvm = Arc::new(jvm);
    let rc_jvm = jvm.clone();

    let g_callback = env.new_global_ref(callback).unwrap();
    let callback_rc = Arc::new(g_callback);
    let rc_callback = callback_rc.clone();

    async_receive(port as u16, move |r| {
        let thread_env = rc_jvm.attach_current_thread().unwrap();
        let callback = rc_callback.as_obj();
        match r {
            Ok(s) => {
                invoke_callback(*thread_env.deref(), callback, s, false).unwrap();
            }
            Err(e) => {
                let format = format!("{:?}", e);
                invoke_callback(*thread_env.deref(), callback, format, true).unwrap();
            }
        }
    });
}

fn invoke_callback(
    env: JNIEnv,
    callback: JObject,
    msg: String,
    error: bool,
) -> jni::errors::Result<()> {
    let jni_str = env.new_string(msg)?;
    env.call_method(
        callback,
        "onResult",
        "(Ljava/lang/String;Z)V",
        &[JValue::Object(jni_str.into()), JValue::Bool(error as u8)],
    )?;
    Ok(())
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Transfer_getLocalIpInfo(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let interfaces = pnet::datalink::interfaces();
    let mut info = String::new();

    let check_loopback = |ips: &Vec<IpNetwork>| {
        for x in ips {
            if x.is_ipv4() {
                if x.ip().is_loopback() {
                    return true;
                }
            }
        }
        false
    };

    for x in interfaces {
        let ip = &x.ips;
        if ip.len() <= 1 || check_loopback(ip) {
            continue;
        }
        let format = format!("{}", x);
        info.push_str(format.as_str());
        info.push('\n');
    }
    let j_string = env.new_string(info).unwrap();
    j_string.into_inner()
}
