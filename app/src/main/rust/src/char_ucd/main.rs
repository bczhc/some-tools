use jni::objects::{JClass, JString, JValue};
use jni::sys::jobject;
use jni::JNIEnv;

use crate::char_ucd::lib;
use crate::jni_helper::GetString;

use super::errors::*;

fn count(env: JNIEnv, src: JString, callback: jobject) -> Result<u32> {
    let src = env.get_string_owned(src)?;
    lib::read_total_count(&src, |i| {
        env.call_method(callback, "progress", "(I)V", &[JValue::Int(i as i32)])
            .unwrap();
    })
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CharUcd_count(
    env: JNIEnv,
    _class: JClass,
    src: JString,
    callback: jobject,
) -> i32 {
    let call = count(env, src, callback);
    (match call {
        Ok(count) => count,
        Err(e) => {
            env.throw(format!("{:?}", e)).unwrap();
            0_u32
        }
    }) as i32
}

fn parse_xml(env: JNIEnv, src: JString, dest: JString, callback: jobject) -> Result<()> {
    let src = env.get_string_owned(src)?;
    let dest = env.get_string_owned(dest)?;

    lib::parse_xml(&src, &dest, |i| {
        env.call_method(callback, "progress", "(I)V", &[JValue::Int(i)])
            .unwrap();
    })?;
    Ok(())
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CharUcd_parseXml(
    env: JNIEnv,
    _class: JClass,
    src: JString,
    dest: JString,
    callback: jobject,
) {
    let call = parse_xml(env, src, dest, callback);
    if let Err(e) = call {
        env.throw(format!("{:?}", e)).unwrap();
    }
}
