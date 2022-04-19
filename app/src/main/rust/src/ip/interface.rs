use jni::objects::JClass;
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use pnet::ipnetwork::IpNetwork;
use std::net::IpAddr;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Ip_getLocalIpObj(env: JNIEnv, _: JClass) -> jlong {
    todo!()
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Ip_ipObjToString(
    env: JNIEnv,
    _: JClass,
    addr: jlong,
) -> jstring {
    let ip_addr = unsafe { &*(addr as *mut IpAddr) };
    let string = format!("{}", ip_addr);
    env.new_string(string).unwrap().into_inner()
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Ip_getLocalIpInfo(env: JNIEnv, _class: JClass) -> jstring {
    let interfaces = pnet::datalink::interfaces();
    let mut info = String::new();

    let check_loopback = |ips: &Vec<IpNetwork>| {
        for x in ips {
            if x.is_ipv4() && x.ip().is_loopback() {
                return true;
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
