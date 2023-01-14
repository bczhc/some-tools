pub mod errors;
pub mod lib;
use errors::*;

use jni::objects::{JClass, JString, JValue};
use jni::sys::{jint, jobject};
use jni::JNIEnv;

use crate::jni_helper::GetString;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CharUcd_count(
    env: JNIEnv,
    _class: JClass,
    src: JString,
    callback: jobject,
) -> i32 {
    let result: Result<u32> = try {
        let src = env.get_string_owned(src)?;
        lib::read_total_count(&src, |i| {
            env.call_method(callback, "progress", "(I)V", &[JValue::Int(i as i32)])
                .unwrap();
        })?
    };
    let result = match result {
        Ok(count) => count,
        Err(e) => {
            env.throw(format!("{:?}", e)).unwrap();
            0_u32
        }
    };
    result as i32
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
    let result: Result<()> = try {
        let src = env.get_string_owned(src)?;
        let dest = env.get_string_owned(dest)?;

        lib::parse_xml(&src, &dest, |i, p| {
            env.call_method(
                callback,
                "progress",
                "(II)V",
                &[JValue::Int(i as jint), JValue::Int(p as jint)],
            )
            .unwrap();
        })?;
    };
    if let Err(e) = result {
        env.throw(format!("{:?}", e)).unwrap();
    }
}
