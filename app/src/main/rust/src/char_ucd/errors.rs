use std::io;
use thiserror::Error;

pub type Result<T> = std::result::Result<T, Error>;

#[derive(Error, Debug)]
pub enum Error {
    #[error("{0}")]
    Sqlite(#[from] rusqlite::Error),
    #[error("{0}")]
    GetString(#[from] crate::jni_helper::GetStringError),
    #[error("{0}")]
    Io(#[from] io::Error),
    #[error("{0}")]
    Zip(#[from] zip::result::ZipError),
}
