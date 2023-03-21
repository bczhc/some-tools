use jni::objects::JClass;
use jni::sys::{jboolean, jlong, jstring};
use jni::JNIEnv;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024ByteSize_toHumanReadable(
    env: JNIEnv,
    _class: JClass,
    size: jlong,
    si_unit: jboolean,
) -> jstring {
    let string = bytesize::ByteSize(size as u64).to_string_as(si_unit != 0);
    let string = env.new_string(string).unwrap();
    string.into_raw()
}
