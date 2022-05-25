use std::str::Utf8Error;

use jni::objects::JString;
use jni::JNIEnv;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum GetStringError {
    #[error("{0}")]
    JniError(#[from] jni::errors::Error),
    #[error("{0}")]
    Utf8Error(#[from] Utf8Error),
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
