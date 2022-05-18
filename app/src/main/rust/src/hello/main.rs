use crate::jni_helper;
use jni::objects::{JClass, JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "C" fn Java_pers_zhc_tools_jni_JNI_00024JniDemo_hello(
    env: JNIEnv,
    _: JClass,
    context: JObject,
    name: JString,
) -> jstring {
    let name: String = env.get_string(name).unwrap().into();
    let content = format!("Hello, {}!", name);
    let s = env.new_string(content.clone()).unwrap();

    jni_helper::toast(env, context, &content).unwrap();

    let s = rusqlite::version();
    let s = env.new_string(s).unwrap();

    s.into_inner()
}
