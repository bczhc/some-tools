#![feature(try_blocks)]
#![feature(yeet_expr)]

extern crate core;

use std::sync::Mutex;
use std::{env, panic};

use crate::jni_helper::jni_log;
use jni::objects::JClass;
use jni::{JNIEnv, JavaVM};
use once_cell::sync::Lazy;

pub static JAVA_VM: Lazy<Mutex<Option<JavaVM>>> = Lazy::new(|| Mutex::new(None));

fn set_up_panic_hook() {
    env::set_var("RUST_BACKTRACE", "1");
    panic::set_hook(Box::new(|i| {
        let result: anyhow::Result<()> = try {
            let guard = JAVA_VM.lock().unwrap();
            let jvm = guard.as_ref().unwrap();
            let mut env = jvm.attach_current_thread().unwrap();
            let msg = format!("Rust panic!!\n{}", i);
            jni_log(&mut env, &msg)?;
            env.throw(msg)?;
        };
        let _ = result;
    }));
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_rustInitialize(env: JNIEnv, _: JClass) {
    JAVA_VM.lock().unwrap().replace(env.get_java_vm().unwrap());
    set_up_panic_hook()
}

pub mod app;
pub mod bitmap;
pub mod byte_size;
pub mod bzip3;
pub mod char;
pub mod char_stat;
pub mod char_ucd;
pub mod compression;
pub mod crash;
pub mod diary;
pub mod email;
pub mod encoding;
pub mod fourier_series;
pub mod hello;
pub mod ip;
pub mod jni_helper;
pub mod lzma;
pub mod signals;
pub mod transfer;
pub mod unicode;
