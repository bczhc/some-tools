use std::fmt::Debug;

use jni::JNIEnv;

pub trait CheckOrThrow {
    fn check_or_throw(&self, env: JNIEnv) -> Result<(), jni::errors::Error>;
}

impl<R, E> CheckOrThrow for Result<R, E>
    where
        E: Debug,
{
    fn check_or_throw(&self, env: JNIEnv) -> Result<(), jni::errors::Error> {
        if let Err(e) = self {
            let string = format!("{:?}", e);
            env.throw(string.as_str())?;
        }
        Ok(())
    }
}

pub trait UnwrapOrThrow<T> {
    fn unwrap_or_throw(self, env: JNIEnv, msg: &str) -> T;
}

impl<T> UnwrapOrThrow<T> for Option<T> {
    fn unwrap_or_throw(self, env: JNIEnv, msg: &str) -> T {
        match self {
            None => {
                env.throw(msg).unwrap();
                unreachable!();
            }
            Some(a) => a,
        }
    }
}
