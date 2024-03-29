use std::str::Utf8Error;

use jni::objects::JString;
use jni::strings::JavaStr;
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
    fn get_string_owned(&mut self, str: JString) -> Result<String, GetStringError>;
}

pub trait JavaStrExt {
    fn to_str_or_throw(&self, env: &mut JNIEnv) -> jni::errors::Result<&str>;
}

impl<'a> GetString for JNIEnv<'a> {
    fn get_string_owned(&mut self, js: JString) -> Result<String, GetStringError> {
        let java_str = self.get_string(&js)?;
        Ok(String::from(java_str.to_str()?))
    }
}

impl<'a, 'b, 'c> JavaStrExt for JavaStr<'a, 'b, 'c> {
    fn to_str_or_throw(&self, env: &mut JNIEnv) -> jni::errors::Result<&str> {
        match self.to_str() {
            Ok(s) => Ok(s),
            Err(e) => {
                env.throw(format!("UTF-8 error: {}", e))?;
                Ok("")
            }
        }
    }
}

#[macro_export]
/// Get Java string
///
/// env, variable name, string object
macro_rules! java_str_var {
    ($env:expr, $var:ident, $s:expr) => {
        let $var = $env.get_string(&$s)?;
        let $var = $var.to_str()?;
    };
}

#[macro_export]
/// Rust string to `jstring`
macro_rules! new_java_string {
    ($env:expr, $s:expr) => {
        $env.new_string($s)?.into_raw()
    };
}
