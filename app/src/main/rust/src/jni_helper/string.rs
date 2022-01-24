use std::str::Utf8Error;

use jni::errors::Error;
use jni::objects::JString;
use jni::JNIEnv;

#[derive(Debug)]
pub enum GetStringError {
    JniError(jni::errors::Error),
    Utf8Error(Utf8Error),
}

pub trait GetString {
    fn get_string_owned(&self, js: JString) -> Result<String, GetStringError>;
}

impl<'a> GetString for JNIEnv<'a> {
    fn get_string_owned(&self, js: JString) -> Result<String, GetStringError> {
        let java_str = self.get_string(js)?;
        Ok(String::from(java_str.to_str()?))
    }
}

impl From<jni::errors::Error> for GetStringError {
    fn from(e: Error) -> Self {
        Self::JniError(e)
    }
}

impl From<Utf8Error> for GetStringError {
    fn from(e: Utf8Error) -> Self {
        Self::Utf8Error(e)
    }
}
