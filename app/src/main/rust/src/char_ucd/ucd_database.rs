use std::path::Path;

use rusqlite::{params, Connection, Statement};

use super::errors::*;

pub struct UcdDatabase<'a> {
    conn: *const Connection,
    insert_stmt: Statement<'a>,
}

macro_rules! ref_raw {
    ($x:expr) => {
        unsafe { &*$x }
    };
}

impl<'a> UcdDatabase<'a> {
    pub fn commit(&self) -> Result<()> {
        ref_raw!(self.conn).execute("COMMIT", params![])?;
        Ok(())
    }
}

impl<'a> UcdDatabase<'a> {
    pub fn new<P: AsRef<Path>>(path: P) -> Result<UcdDatabase<'a>> {
        let conn = Connection::open(path)?;
        let conn = Box::into_raw(Box::new(conn));
        Self::init(ref_raw!(conn))?;

        let insert_stmt =
            ref_raw!(conn).prepare("INSERT INTO ucd (codepoint, properties) VALUES (?, ?)")?;
        Ok(Self { conn, insert_stmt })
    }

    fn init(conn: &Connection) -> Result<()> {
        conn.execute(
            r"CREATE TABLE IF NOT EXISTS ucd
(
    codepoint  INTEGER PRIMARY KEY,
    properties TEXT NOT NULL
)",
            params![],
        )?;
        Ok(())
    }

    #[inline]
    pub fn insert(&mut self, codepoint: u32, properties: &str) -> Result<()> {
        self.insert_stmt.execute(params![codepoint, properties])?;
        Ok(())
    }
}
