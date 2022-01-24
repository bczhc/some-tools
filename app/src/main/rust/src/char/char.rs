use std::mem::transmute;
use std::slice::from_raw_parts;

use jni::objects::JClass;
use jni::sys::{jbyteArray, jint, jshortArray};
use jni::JNIEnv;

use crate::jni_helper::UnwrapOrThrow;

fn get_char(env: JNIEnv, codepoint: u32) -> char {
    char::from_u32(codepoint as u32).unwrap_or_throw(env, "Invalid codepoint")
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Char_getUtf8Len(
    env: JNIEnv,
    _class: JClass,
    codepoint: jint,
) -> jint {
    get_char(env, codepoint as u32).len_utf8() as jint
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Char_getUtf16Len(
    env: JNIEnv,
    _class: JClass,
    codepoint: jint,
) -> jint {
    get_char(env, codepoint as u32).len_utf16() as jint
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Char_encodeUTF8(
    env: JNIEnv,
    _class: JClass,
    codepoint: jint,
    arr: jbyteArray,
    start: jint,
) {
    let c = get_char(env, codepoint as u32);
    let mut buf = vec![0_u8; c.len_utf8()];
    c.encode_utf8(&mut buf);

    env.set_byte_array_region(arr, start as i32, unsafe { transmute(&buf[..]) })
        .unwrap();
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Char_encodeUTF16(
    env: JNIEnv,
    _class: JClass,
    codepoint: jint,
    arr: jshortArray,
    start: jint,
) {
    let c = get_char(env, codepoint as u32);
    let mut buf = vec![0_u16; c.len_utf16()];
    c.encode_utf16(&mut buf);

    env.set_short_array_region(arr, start as i32, unsafe { transmute(&buf[..]) })
        .unwrap();
}
