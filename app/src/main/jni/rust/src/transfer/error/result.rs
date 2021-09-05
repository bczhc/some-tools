use std::any::Any;
use std::str::Utf8Error;
use std::string::FromUtf8Error;

#[derive(Debug)]
pub enum Error {
    IoError(std::io::Error),
    ThreadError(ThreadError),
    JniError(jni::errors::Error),
    Utf8Error(std::str::Utf8Error),
    ContractError(ContractError),
}

#[derive(Debug)]
pub enum ContractError {
    InvalidHeader,
    InvalidMark(u8),
    UnsupportedType,
}

type ThreadError = Box<dyn Any + Send + 'static>;

pub type Result<T> = std::result::Result<T, Error>;

impl From<std::io::Error> for Error {
    fn from(e: std::io::Error) -> Self {
        Error::IoError(e)
    }
}

impl From<ThreadError> for Error {
    fn from(e: ThreadError) -> Self {
        Error::ThreadError(e)
    }
}

impl From<jni::errors::Error> for Error {
    fn from(e: jni::errors::Error) -> Self {
        Error::JniError(e)
    }
}

impl From<std::str::Utf8Error> for Error {
    fn from(e: Utf8Error) -> Self {
        Error::Utf8Error(e)
    }
}

impl From<ContractError> for Error {
    fn from(e: ContractError) -> Self {
        Error::ContractError(e)
    }
}

impl From<FromUtf8Error> for Error {
    fn from(e: FromUtf8Error) -> Self {
        Error::Utf8Error(e.utf8_error())
    }
}
