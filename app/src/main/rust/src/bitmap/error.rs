use image::ImageError;
use std::str::Utf8Error;

use ndk::bitmap::BitmapError;

pub type Result<T> = std::result::Result<T, Error>;

#[derive(Debug)]
pub enum Error {
    BitmapError(BitmapError),
    Utf8Error(Utf8Error),
    ImageError(ImageError),
    JniError(jni::errors::Error),
}

impl From<BitmapError> for Error {
    fn from(e: BitmapError) -> Self {
        Self::BitmapError(e)
    }
}

impl From<Utf8Error> for Error {
    fn from(e: Utf8Error) -> Self {
        Self::Utf8Error(e)
    }
}

impl From<ImageError> for Error {
    fn from(e: ImageError) -> Self {
        Self::ImageError(e)
    }
}

impl From<jni::errors::Error> for Error {
    fn from(e: jni::errors::Error) -> Self {
        Self::JniError(e)
    }
}
