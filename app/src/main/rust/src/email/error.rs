use std::str::Utf8Error;

use lettre::address::AddressError;

use crate::email::error::lettre_error::{General, Transport};

pub mod lettre_error {
    pub type General = lettre::error::Error;
    pub type Transport = lettre::transport::smtp::Error;
}

#[derive(Debug)]
pub enum LettreError {
    General(lettre_error::General),
    Transport(lettre_error::Transport),
}

#[derive(Debug)]
pub enum Error {
    Utf8(std::str::Utf8Error),
    Jni(jni::errors::Error),
    Address(AddressError),
    Lettre(LettreError),
}

pub type Result<T> = std::result::Result<T, Error>;

impl From<Utf8Error> for Error {
    fn from(e: Utf8Error) -> Self {
        Error::Utf8(e)
    }
}

impl From<jni::errors::Error> for Error {
    fn from(e: jni::errors::Error) -> Self {
        Error::Jni(e)
    }
}

impl From<AddressError> for Error {
    fn from(e: AddressError) -> Self {
        Error::Address(e)
    }
}

impl From<lettre_error::General> for LettreError {
    fn from(e: General) -> Self {
        LettreError::General(e)
    }
}

impl From<lettre_error::Transport> for LettreError {
    fn from(e: Transport) -> Self {
        LettreError::Transport(e)
    }
}

impl From<lettre_error::General> for Error {
    fn from(e: General) -> Self {
        e.into()
    }
}

impl From<lettre_error::Transport> for Error {
    fn from(e: Transport) -> Self {
        e.into()
    }
}

impl From<LettreError> for Error {
    fn from(e: LettreError) -> Self {
        Error::Lettre(e)
    }
}
