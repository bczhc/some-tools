pub enum Mark {
    File,
    Text,
    End,
}

pub trait EnumU8Value {
    #[inline]
    fn u8_value(&self) -> u8;
}

pub trait FromValue {
    #[inline]
    fn from_value(value: u8) -> Option<Self>
    where
        Self: Sized;
}

pub enum Status {
    Ok,
}

impl EnumU8Value for Mark {
    fn u8_value(&self) -> u8 {
        match self {
            Mark::File => 1,
            Mark::Text => 2,
            Mark::End => 3,
        }
    }
}

impl FromValue for Mark {
    fn from_value(value: u8) -> Option<Self> {
        match value {
            1 => Some(Mark::File),
            2 => Some(Mark::Text),
            3 => Some(Mark::End),
            _ => None,
        }
    }
}

impl EnumU8Value for Status {
    fn u8_value(&self) -> u8 {
        match self {
            Status::Ok => 0,
        }
    }
}

impl FromValue for Status {
    fn from_value(value: u8) -> Option<Self> {
        match value {
            0 => Some(Status::Ok),
            _ => None,
        }
    }
}

pub const HEADER: &[u8; 8] = b"bczhc\0\0\0";
