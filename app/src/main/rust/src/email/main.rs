use jni::objects::{JClass, JString};
use jni::strings::JavaStr;
use jni::sys::{jobjectArray, jsize};
use jni::JNIEnv;
use lettre::transport::smtp::authentication::Credentials;
use lettre::{Message, SmtpTransport, Transport};

use crate::email::error::Result;
use crate::jni_helper::CheckOrThrow;

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Email_send(
    env: JNIEnv,
    _class: JClass,
    smtp_server: JString,
    username: JString,
    password: JString,
    from: JString,
    to: jobjectArray,
    cc: jobjectArray,
    subject: JString,
    body: JString,
) {
    let result = run(
        env,
        smtp_server,
        username,
        password,
        from,
        to,
        cc,
        subject,
        body,
    );
    result.check_or_throw(env).unwrap();
}

#[allow(clippy::too_many_arguments)]
fn run(
    env: JNIEnv,
    smtp_server: JString,
    username: JString,
    password: JString,
    from: JString,
    to: jobjectArray,
    cc: jobjectArray,
    subject: JString,
    body: JString,
) -> Result<()> {
    let smtp_server = env.get_and_to_string(smtp_server)?;
    let username = env.get_and_to_string(username)?;
    let password = env.get_and_to_string(password)?;
    let from = env.get_and_to_string(from)?;
    let subject = env.get_and_to_string(subject)?;
    let body = env.get_and_to_string(body)?;
    let to = get_java_string_array(env, to)?;
    let cc = if cc.is_null() {
        Vec::new()
    } else {
        get_java_string_array(env, cc)?
    };

    let credentials = Credentials::new(username, password);

    let mut message_builder = Message::builder().from(from.parse()?);
    for to in to {
        message_builder = message_builder.to(to.parse()?);
    }
    for cc in cc {
        message_builder = message_builder.cc(cc.parse()?);
    }
    let message = message_builder.subject(subject).body(body)?;

    let smtp_transport = SmtpTransport::relay(smtp_server.as_str())?
        .credentials(credentials)
        .build();

    smtp_transport.send(&message)?;
    Ok(())
}

fn get_java_string_array(env: JNIEnv, arr: jobjectArray) -> Result<Vec<String>> {
    let length = env.get_array_length(arr)? as usize;
    let mut vec = Vec::with_capacity(length);
    for i in 0..length {
        let object = env.get_object_array_element(arr, i as jsize)?;
        let java_str = env.get_string(object.into())?;

        let s = java_str.to_string()?;
        vec.push(s);
    }
    Ok(vec)
}

type ToJavaStringResult = Result<String>;

trait ToString {
    fn to_string(&self) -> ToJavaStringResult;
}

impl ToString for JavaStr<'_, '_> {
    fn to_string(&self) -> ToJavaStringResult {
        let str = self.to_str()?;
        Ok(String::from(str))
    }
}

trait GetString {
    fn get_and_to_string(&self, java_string: JString) -> ToJavaStringResult;
}

impl GetString for JNIEnv<'_> {
    fn get_and_to_string(&self, java_string: JString) -> ToJavaStringResult {
        let java_str = self.get_string(java_string)?;
        java_str.to_string()
    }
}
