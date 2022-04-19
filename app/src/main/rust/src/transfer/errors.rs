pub type Result<T> = std::result::Result<T, Error>;

use crate::jni_helper::GetStringError;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum Error {
    #[error("IllegalArgument")]
    IllegalArgument,
    #[error("{0}")]
    Io(#[from] std::io::Error),
    #[error("{0}")]
    Jni(#[from] jni::errors::Error),
    #[error("Unexpected EOF")]
    UnexpectedEof,
    #[error("InvalidHeader")]
    InvalidHeader,
    #[error("InvalidMark")]
    InvalidMark,
    #[error("{0}")]
    Utf8Error(#[from] std::str::Utf8Error),
    #[error("InvalidPathName")]
    InvalidPathName,
}

impl From<GetStringError> for Error {
    fn from(e: GetStringError) -> Self {
        match e {
            GetStringError::JniError(e) => Error::Jni(e),
            GetStringError::Utf8Error(e) => Error::Utf8Error(e),
        }
    }
}
