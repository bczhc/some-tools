use digest::Digest;
use jni::objects::{JByteArray, JClass};
use jni::sys::jstring;
use jni::JNIEnv;
use sha2::Sha256;

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Digest_sha256(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jstring {
    let data = env.convert_byte_array(data).unwrap();
    let mut hasher = Sha256::new();
    hasher.update(&data);
    let hash: [u8; 256 / 8] = hasher.finalize().into();
    let hex = hex::encode(hash);
    env.new_string(hex).unwrap().into_raw()
}
