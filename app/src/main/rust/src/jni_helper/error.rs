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

pub const JNI_ERROR_OCCURRED_MSG: &str = "JNI error occurred";

pub macro unwrap_or_throw_result($env:expr, $x:expr, $default:expr) {
    match $x {
        Ok(x) => x,
        Err(e) => {
            let string = format!("{:?}", e);
            let string = string.as_str();
            jni_log($env, &format!("throw exception:\n{}", string)).expect(JNI_ERROR_OCCURRED_MSG);
            $env.throw(string).expect(JNI_ERROR_OCCURRED_MSG);
            return $default;
        }
    }
}

pub macro unwrap_or_throw_option($env:expr, $x:expr, $default:expr) {
    match $x {
        Some(x) => x,
        None => {
            let error_msg = "unwrap on `None`";
            jni_log($env, error_msg).expect(JNI_ERROR_OCCURRED_MSG);
            $env.throw(error_msg).expect(JNI_ERROR_OCCURRED_MSG);
            return $default;
        }
    }
}

pub macro expect_or_throw_option($env:expr, $x:expr, $default:expr, $msg:expr) {
    match $x {
        None => {
            jni_log($env, &format!("Expect msg: {}", $msg)).expect(JNI_ERROR_OCCURRED_MSG);
            $env.throw($msg).expect(JNI_ERROR_OCCURRED_MSG);
            return $default;
        }
        Some(x) => x,
    }
}
