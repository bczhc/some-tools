use jni::objects::{JByteArray, JClass};
use jni::sys::jstring;
use jni::JNIEnv;

use crate::jni_helper::{jobject_null, unwrap_or_throw_result};

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Utf8_fromBytesLossy(
    mut env: JNIEnv,
    _class: JClass,
    bytes: JByteArray,
) -> jstring {
    let result: anyhow::Result<jstring> = try {
        let bytes = env.convert_byte_array(bytes)?;
        let str = &*String::from_utf8_lossy(&bytes);
        env.new_string(str)?.into_raw()
    };
    unwrap_or_throw_result!(&mut env, result, jobject_null())
}
