use jni::objects::{JClass, JString};
use jni::strings::JavaStr;
use jni::sys::jint;
use jni::JNIEnv;
use unicode_segmentation::UnicodeSegmentation;

fn string_to_str_throw<'a>(mut env: JNIEnv, str: &'a JavaStr) -> jni::errors::Result<&'a str> {
    match str.to_str() {
        Ok(s) => Ok(s),
        Err(e) => {
            env.throw(format!("UTF-8 error: {}", e))?;
            Ok("")
        }
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CharStat_codepointCount(
    mut env: JNIEnv,
    _class: JClass,
    text: JString,
) -> jint {
    let text = env.get_string(&text).unwrap();
    let text = string_to_str_throw(env, &text).unwrap();
    text.chars().count() as jint
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CharStat_graphemeCount(
    mut env: JNIEnv,
    _class: JClass,
    text: JString,
) -> jint {
    let text = env.get_string(&text).unwrap();
    let text = string_to_str_throw(env, &text).unwrap();
    text.graphemes(true).count() as jint
}
