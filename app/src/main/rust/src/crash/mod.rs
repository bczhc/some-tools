use crate::jni_helper::CheckOrThrow;
use anyhow::anyhow;
use backtrace::Backtrace;
use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CrashTest_panic(_env: JNIEnv, _: JClass) {
    let _i = "a".parse::<u32>().expect("Oops!");
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CrashTest_throwException(
    mut env: JNIEnv,
    _: JClass,
) {
    let result: Result<(), anyhow::Error> = Err(anyhow!("Aha!"));
    result.check_or_throw(&mut env).unwrap();
    // unreachable!();
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024CrashTest_testBacktrace(
    env: JNIEnv,
    _: JClass,
) -> jstring {
    #[inline(never)]
    fn f1() -> String {
        f2()
    }

    #[inline(never)]
    fn f2() -> String {
        f3()
    }

    #[inline(never)]
    fn f3() -> String {
        let backtrace = Backtrace::new();
        format!("{:?}", backtrace)
    }

    let backtrace = f1();
    env.new_string(backtrace).unwrap().into_raw()
}
