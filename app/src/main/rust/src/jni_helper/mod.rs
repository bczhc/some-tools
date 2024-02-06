pub use error::*;
use jni::objects::JObject;
use jni::sys::jobject;
pub use log::*;
pub use string::*;
pub use toast::*;

mod error;
mod log;
mod string;
mod toast;

pub fn jobject_null() -> jobject {
    JObject::null().into_raw()
}
