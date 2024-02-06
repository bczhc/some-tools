use std::mem::transmute;

use jni::objects::{JByteArray, JClass, JShortArray};
use jni::sys::jint;
use jni::JNIEnv;

use crate::jni_helper::ExpectOrThrow;

fn get_char(env: &mut JNIEnv, codepoint: u32) -> char {
    char::from_u32(codepoint).expect_or_throw(env, char::default(), "Invalid codepoint")
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Char_getUtf8Len(
    mut env: JNIEnv,
    _class: JClass,
    codepoint: jint,
) -> jint {
    get_char(&mut env, codepoint as u32).len_utf8() as jint
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Char_getUtf16Len(
    mut env: JNIEnv,
    _class: JClass,
    codepoint: jint,
) -> jint {
    get_char(&mut env, codepoint as u32).len_utf16() as jint
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Char_encodeUTF8(
    mut env: JNIEnv,
    _class: JClass,
    codepoint: jint,
    arr: JByteArray,
    start: jint,
) {
    let c = get_char(&mut env, codepoint as u32);
    let mut buf = vec![0_u8; c.len_utf8()];
    c.encode_utf8(&mut buf);

    env.set_byte_array_region(arr, start, unsafe { transmute(&buf[..]) })
        .unwrap();
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Char_encodeUTF16(
    mut env: JNIEnv,
    _class: JClass,
    codepoint: jint,
    arr: JShortArray,
    start: jint,
) {
    let c = get_char(&mut env, codepoint as u32);
    let mut buf = vec![0_u16; c.len_utf16()];
    c.encode_utf16(&mut buf);

    env.set_short_array_region(arr, start, unsafe { transmute(&buf[..]) })
        .unwrap();
}
