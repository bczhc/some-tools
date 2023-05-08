use jni::objects::JClass;
use jni::sys::jobject;
use jni::JNIEnv;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024JniDemo_call2(
    mut env: JNIEnv,
    _: JClass,
) -> jobject {
    let result = env.new_string("hello").unwrap();
    let _ = "asd".parse::<u32>().unwrap();
    result.into_raw()
}
