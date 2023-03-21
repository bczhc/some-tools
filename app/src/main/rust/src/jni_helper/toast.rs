use jni::objects::{JObject, JValue};
use jni::JNIEnv;

pub fn toast(env: &mut JNIEnv, context: &JObject, content: &str) -> jni::errors::Result<()> {
    let content = env.new_string(content)?;
    let class = env.find_class("pers/zhc/tools/utils/ToastUtils")?;
    env.call_static_method(
        class,
        "show",
        "(Landroid/content/Context;Ljava/lang/CharSequence;)V",
        &[JValue::Object(context), JValue::Object(&content.into())],
    )?;
    Ok(())
}
