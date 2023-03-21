use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong};
use jni::JNIEnv;

use crate::jni_helper::CheckOrThrow;
use crate::transfer::lib;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Transfer_asyncStartServer(
    mut env: JNIEnv,
    _: JClass,
    port: jint,
    saving_path: JString,
    callback: JObject,
) -> jlong {
    let result = lib::async_start_server(&mut env, port, saving_path, callback);
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
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Transfer_send(
    mut env: JNIEnv,
    _class: JClass,
    socket_addr: JString,
    mark: jint,
    path: JString,
    callback: JObject,
) {
    let result = lib::send(&mut env, socket_addr, mark, path, callback);
    result.check_or_throw(&mut env).unwrap();
}
