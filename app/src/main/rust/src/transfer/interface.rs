use crate::jni_helper::CheckOrThrow;
use crate::transfer::lib;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jobject};
use jni::JNIEnv;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Transfer_asyncStartServer(
    env: JNIEnv,
    _: JClass,
    port: jint,
    saving_path: JString,
    callback: jobject,
) -> jlong {
    let result = lib::async_start_server(env, port, saving_path, callback);
    match result {
        Ok(id) => id,
        Err(e) => {
            env.throw(format!("{:?}", e)).unwrap();
            0
        }
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Transfer_send(
    env: JNIEnv,
    _class: JClass,
    socket_addr: JString,
    mark: jint,
    path: JString,
    callback: jobject,
) {
    let result = lib::send(env, socket_addr, mark, path, callback);
    result.check_or_throw(env).unwrap();
}
