use jni::objects::JValue;
use jni::JNIEnv;

pub fn log(env: JNIEnv, tag: &str, msg: &str) -> jni::errors::Result<()> {
    let tag = env.new_string(tag)?;
    let msg = env.new_string(msg)?;

    let class = env.find_class("android/util/Log")?;
    env.call_static_method(
        class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[JValue::Object(tag.into()), JValue::Object(msg.into())],
    )?;
    Ok(())
}
