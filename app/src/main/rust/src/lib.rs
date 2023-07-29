#![feature(try_blocks)]
#![feature(yeet_expr)]

extern crate core;

use std::fs::File;
use std::io::Write;
use std::path::PathBuf;
use std::sync::Mutex;
use std::{env, io, panic};

use backtrace::Backtrace;
use bczhc_lib::io::OpenOrCreate;
use bczhc_lib::mutex_lock;
use chrono::Local;
use jni::objects::{JClass, JObjectArray, JString};
use jni::{JNIEnv, JavaVM};
use once_cell::sync::Lazy;

use crate::jni_helper::{jni_log, CheckOrThrow};

type LazyStatic<T> = Lazy<Mutex<Option<T>>>;
macro_rules! lazy_static_init {
    () => {
        Lazy::new(|| Mutex::new(None))
    };
}

pub static JAVA_VM: LazyStatic<JavaVM> = lazy_static_init!();
pub static STATIC_FIELDS: LazyStatic<StaticFields> = lazy_static_init!();

pub struct StaticFields {
    pub crash_log_dir: String,
}

fn set_up_panic_hook() {
    env::set_var("RUST_BACKTRACE", "1");
    panic::set_hook(Box::new(|i| {
        let backtrace = Backtrace::new();
        let backtrace = format!("{:?}", backtrace);

        let result: anyhow::Result<()> = try {
            let guard = JAVA_VM.lock().unwrap();
            let jvm = guard.as_ref().unwrap();
            let mut env = jvm.attach_current_thread().unwrap();
            let msg = format!("Rust panic!!\n{}\nBacktrace:\n{}", i, backtrace);
            jni_log(&mut env, &msg)?;
            let _ = write_panic_info(&msg);
        };
        let _ = result;
    }));
}

fn write_panic_info(info: &str) -> io::Result<()> {
    let format = Local::now().format("%Y%m%d_%H%M%S.%3f");
    let guard = mutex_lock!(STATIC_FIELDS);
    let fields = guard.as_ref().unwrap();
    let mut path = PathBuf::from(&fields.crash_log_dir);
    drop(guard);
    path.push(format!("rust_crash_{}.txt", format));

    let mut file = File::open_or_create(path)?;
    file.write_all(info.as_bytes())?;

    Ok(())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_rustInitialize(env: JNIEnv, _: JClass) {
    JAVA_VM.lock().unwrap().replace(env.get_java_vm().unwrap());
    set_up_panic_hook()
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_rustSetUpStaticFields(
    mut env: JNIEnv,
    _: JClass,
    strings: JObjectArray,
) {
    let result: anyhow::Result<()> = try {
        macro_rules! strings_index {
            ($index:literal) => {{
                let string = env.get_object_array_element(&strings, $index)?;
                let string = JString::from(string);
                java_str_var!(env, string, string);
                String::from(string)
            }};
        }

        let fields = StaticFields {
            crash_log_dir: strings_index!(0),
        };
        mutex_lock!(STATIC_FIELDS).replace(fields);
    };
    result.check_or_throw(&mut env).unwrap();
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
