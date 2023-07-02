use std::fmt::Debug;

use crate::jni_helper::jni_log;
use jni::JNIEnv;

pub trait CheckOrThrow {
    fn check_or_throw(&self, env: &mut JNIEnv) -> Result<(), jni::errors::Error>;
}

impl<R, E> CheckOrThrow for Result<R, E>
where
    E: Debug,
{
    fn check_or_throw(&self, env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
        if let Err(e) = self {
            let string = format!("{:?}", e);
            let string = string.as_str();
            jni_log(env, &format!("throw exception:\n{}", string))?;
            env.throw(string)?;
        }
        Ok(())
    }
}

pub trait UnwrapOrThrow<T> {
    fn unwrap_or_throw(self, env: &mut JNIEnv, msg: &str) -> T;
}

impl<T> UnwrapOrThrow<T> for Option<T> {
    fn unwrap_or_throw(self, env: &mut JNIEnv, msg: &str) -> T {
        match self {
            None => {
                env.throw(msg).unwrap();
                unreachable!();
            }
            Some(a) => a,
        }
    }
}
