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
