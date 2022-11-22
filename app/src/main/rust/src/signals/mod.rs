use crate::jni_helper::{jni_log, CheckOrThrow};
use jni::objects::JClass;
use jni::sys::{jint, jintArray, jobjectArray, jsize};
use jni::JNIEnv;
use libc::raise;
use once_cell::sync::Lazy;
use std::error::Error;
use std::ptr::null_mut;

fn signals() -> Vec<(String, i32)> {
    signal::Signal::iterator()
        .map(|x| (format!("{}", x), x as i32))
        .collect::<Vec<_>>()
}

static SIGNALS: Lazy<Vec<(String, i32)>> = Lazy::new(signals);

#[allow(non_snake_case)]
#[no_mangle]
pub fn Java_pers_zhc_tools_jni_JNI_00024Signals_getSignalNames(
    env: JNIEnv,
    _: JClass,
) -> jobjectArray {
    fn names(env: JNIEnv) -> Result<jobjectArray, Box<dyn Error>> {
        let mut vec = Vec::new();
        for x in SIGNALS.iter() {
            let s = env.new_string(&x.0)?;
            vec.push(s.into_inner());
        }
        let jstring_class = env.get_object_class(vec[0])?;
        let array = env.new_object_array(SIGNALS.len() as jsize, jstring_class, null_mut())?;
        for (i, o) in vec.into_iter().enumerate() {
            env.set_object_array_element(array, i as jsize, o)?;
        }
        Ok(array)
    }

    let result = names(env);
    result.check_or_throw(env).unwrap();
    match result {
        Ok(a) => a,
        Err(_) => null_mut(),
    }
}

#[allow(non_snake_case)]
#[no_mangle]
pub fn Java_pers_zhc_tools_jni_JNI_00024Signals_getSignalInts(env: JNIEnv, _: JClass) -> jintArray {
    fn ints(env: JNIEnv) -> Result<jintArray, Box<dyn Error>> {
        let int_array = env.new_int_array(SIGNALS.len() as jsize)?;
        let ints = SIGNALS.iter().map(|x| x.1 as jint).collect::<Vec<_>>();
        env.set_int_array_region(int_array, 0, &ints)?;
        Ok(int_array)
    }
    let result = ints(env);
    jni_log(env, &format!("{:?}", result)).unwrap();
    result.check_or_throw(env).unwrap();
    match result {
        Ok(a) => a,
        Err(_) => null_mut(),
    }
}

#[allow(non_snake_case)]
#[no_mangle]
pub fn Java_pers_zhc_tools_jni_JNI_00024Signals_raise(_env: JNIEnv, _: JClass, signal: jint) {
    unsafe {
        raise(signal as libc::c_int);
    }
}
