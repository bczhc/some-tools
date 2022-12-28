use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, jstring};
use jni::JNIEnv;

use crate::unicode::grapheme::Graphemes;

mod grapheme;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_newIterator(
    env: JNIEnv,
    _class: JClass,
    text: JString,
) -> jlong {
    let text = env.get_string(text).unwrap();
    let graphemes = match Graphemes::new(text) {
        Ok(x) => x,
        Err(e) => {
            env.throw(format!("UTF-8 error: {}", e)).unwrap();
            return 0;
        }
    };

    let graphemes = Box::new(graphemes);
    Box::into_raw(graphemes) as jlong
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_hasNext(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jboolean {
    unsafe { &mut *(addr as *mut Graphemes) }.has_next() as jboolean
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_next(
    env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jstring {
    let grapheme = unsafe { &mut *(addr as *mut Graphemes) }.next();
    env.new_string(grapheme).unwrap().into_inner()
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_release(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) {
    unsafe {
        drop(Box::from_raw(addr as *mut Graphemes));
    }
}
