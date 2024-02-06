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

const JNI_ERROR_OCCURRED_MSG: &str = "JNI failure occurred";

pub trait UnwrapOrThrow<T> {
    fn unwrap_or_throw(self, env: &mut JNIEnv, default: T) -> T;
}

impl<T, E> UnwrapOrThrow<T> for Result<T, E>
where
    E: Debug,
{
    fn unwrap_or_throw(self, env: &mut JNIEnv, default: T) -> T {
        match self {
            Ok(r) => r,
            Err(e) => {
                let string = format!("{:?}", e);
                let string = string.as_str();
                jni_log(env, &format!("throw exception:\n{}", string))
                    .expect(JNI_ERROR_OCCURRED_MSG);
                env.throw(string).expect(JNI_ERROR_OCCURRED_MSG);
                default
            }
        }
    }
}

impl<T> UnwrapOrThrow<T> for Option<T> {
    fn unwrap_or_throw(self, env: &mut JNIEnv, default: T) -> T {
        match self {
            None => {
                let msg = "unwrap on `None`";
                jni_log(env, msg).expect(JNI_ERROR_OCCURRED_MSG);
                env.throw(msg).expect(JNI_ERROR_OCCURRED_MSG);
                default
            }
            Some(r) => r,
        }
    }
}

pub trait ExpectOrThrow<T> {
    fn expect_or_throw(self, env: &mut JNIEnv, default: T, msg: &str) -> T;
}

impl<T> ExpectOrThrow<T> for Option<T> {
    fn expect_or_throw(self, env: &mut JNIEnv, default: T, msg: &str) -> T {
        match self {
            None => {
                env.throw(msg).expect(JNI_ERROR_OCCURRED_MSG);
                default
            }
            Some(a) => a,
        }
    }
}

impl<T, E> ExpectOrThrow<T> for Result<T, E> {
    fn expect_or_throw(self, env: &mut JNIEnv, default: T, msg: &str) -> T {
        match self {
            Ok(r) => r,
            Err(_) => {
                env.throw(msg).expect(JNI_ERROR_OCCURRED_MSG);
                default
            }
        }
    }
}
