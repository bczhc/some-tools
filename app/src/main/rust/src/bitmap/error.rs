use image::ImageError;
use std::str::Utf8Error;

use ndk::bitmap::BitmapError;

pub type Result<T> = std::result::Result<T, Error>;

#[derive(Debug)]
pub enum Error {
    Bitmap(BitmapError),
    Utf8(Utf8Error),
    Image(ImageError),
    Jni(jni::errors::Error),
}

impl From<BitmapError> for Error {
    fn from(e: BitmapError) -> Self {
        Self::Bitmap(e)
    }
}

impl From<Utf8Error> for Error {
    fn from(e: Utf8Error) -> Self {
        Self::Utf8(e)
    }
}

impl From<ImageError> for Error {
    fn from(e: ImageError) -> Self {
        Self::Image(e)
    }
}

impl From<jni::errors::Error> for Error {
    fn from(e: jni::errors::Error) -> Self {
        Self::Jni(e)
    }
}
