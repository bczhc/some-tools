use crate::JAVA_VM;
use bczhc_lib::mutex_lock;
use jni::objects::JValue;
use jni::JNIEnv;

const JNI_LOG_TAG: &str = "jni-log";

pub fn log(env: &mut JNIEnv, tag: &str, msg: &str) -> jni::errors::Result<()> {
    let tag = env.new_string(tag)?;
    let msg = env.new_string(msg)?;

    let class = env.find_class("android/util/Log")?;
    env.call_static_method(
        class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[JValue::Object(&tag.into()), JValue::Object(&msg.into())],
    )?;
    Ok(())
}

pub fn jni_log(env: &mut JNIEnv, msg: &str) -> jni::errors::Result<()> {
    log(env, JNI_LOG_TAG, msg)
}

pub fn jni_log_global(msg: &str) -> jni::errors::Result<()> {
    let guard = mutex_lock!(JAVA_VM);
    let jvm = guard.as_ref().unwrap();
    let mut env = jvm.attach_current_thread()?;
    jni_log(&mut env, msg)?;
    Ok(())
}
