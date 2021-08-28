use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
#[allow(non_snake_case)]
fn Java_pers_zhc_tools_jni_JNI_00024JniDemo_hello(env: JNIEnv, _: JClass, name: JString) -> jstring {
    let name: String = env.get_string(name).unwrap().into();
    let s = env.new_string(format!("Hello, {}!", name)).unwrap();
    s.into_inner()
}