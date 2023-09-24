#![allow(incomplete_features, const_evaluatable_unchecked)]
#![feature(generic_const_exprs)]

use digest::{Digest, Reset};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jint, jsize, jstring};
use jni::JNIEnv;
use sha2::Sha256;

mod lib;

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Digest_sha256(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
    iterations: jint,
) -> jbyteArray {
    let data = env.convert_byte_array(data).unwrap();
    let hash = lib::fixed_output_hash::<Sha256, _>(data.as_slice(), iterations as u64).unwrap();
    env.byte_array_from_slice(&hash).unwrap().into_raw()
}
