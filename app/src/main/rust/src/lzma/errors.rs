pub type Result<T> = std::result::Result<T, Error>;

use thiserror::Error;

#[derive(Error, Debug)]
pub enum Error {
    #[error("{0}")]
    Jni(#[from] jni::errors::Error),
    #[error("{0}")]
    Lzma(#[from] lzma_rs::error::Error),
}
