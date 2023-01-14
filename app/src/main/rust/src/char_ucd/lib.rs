use std::collections::{HashMap, HashSet};
use std::fs::File;
use std::io;
use std::io::{BufReader, ErrorKind, Read};
use std::path::Path;

use quick_xml::events::attributes::Attributes;
use quick_xml::events::Event;
use rusqlite::{params, Connection};
use zip::read::ZipFile;
use zip::ZipArchive;

use super::errors::*;

pub fn read_total_count<F>(zip_path: &str, callback: F) -> Result<u32>
where
    F: Fn(u32),
{
    let mut total = 0_u32;

    let archive = ZipArchive::new(File::open(zip_path)?)?;
    let mut archive = Box::new(archive);
    assert_eq!(archive.len(), 1);
    let zip_file = archive.by_index(0)?;

    let mut xml_reader = quick_xml::Reader::from_reader(BufReader::new(zip_file));
    xml_reader.trim_text(true);

    let mut buf = Vec::new();
    let mut repertoire_enter = false;
    loop {
        let event = xml_reader.read_event_into(&mut buf);
        match event {
            Ok(Event::Start(ref e)) | Ok(Event::Empty(ref e)) => {
                let name_binary = e.name();
                if repertoire_enter {
                    if name_binary.as_ref() == b"char"
                        && e.attributes().next().unwrap().unwrap().key.as_ref() == b"cp"
                    {
                        total += 1;
                        callback(total);
                    }
                } else if name_binary.as_ref() == b"repertoire" {
                    repertoire_enter = true;
                }
            }
            Ok(Event::End(ref e)) => {
                if repertoire_enter && e.name().as_ref() == b"repertoire" {
                    break;
                }
            }
            Ok(Event::Eof) => {
                unreachable!()
            }
            Err(e) => {
                panic!("Reading XML error: {}", e);
            }
            _ => {}
        }
    }
    callback(total);
    Ok(total)
}

fn open_zip<'a, P: AsRef<Path>>(path: P) -> io::Result<XmlZip<'a>> {
    XmlZip::new(path)
}

struct XmlZip<'a> {
    archive: *mut ZipArchive<File>,
    zip_reader: ZipFile<'a>,
}

impl<'a> XmlZip<'a> {
    fn new<P: AsRef<Path>>(zip_path: P) -> io::Result<Self> {
        let archive = ZipArchive::new(File::open(zip_path)?)?;
        if archive.len() != 1 {
            return Err(io::Error::new(ErrorKind::Other, "Unexpected zip file"));
        }

        let archive = Box::into_raw(Box::new(archive));
        let zip_reader = unsafe { (*archive).by_index(0)? };
        Ok(Self {
            archive,
            zip_reader,
        })
    }
}

impl<'a> Drop for XmlZip<'a> {
    fn drop(&mut self) {
        unsafe {
            drop(Box::from_raw(self.archive));
        }
    }
}

impl<'a> Read for XmlZip<'a> {
    fn read(&mut self, buf: &mut [u8]) -> io::Result<usize> {
        self.zip_reader.read(buf)
    }
}

fn stat_attributes<F>(
    zip_path: &str,
    callback: F,
) -> io::Result<(Vec<String>, HashMap<String, usize>)>
where
    F: Fn(u32),
{
    let xml_zip = open_zip(zip_path)?;
    let reader = BufReader::new(xml_zip);

    let mut count = 0_u32;

    let mut xml_reader = quick_xml::Reader::from_reader(reader);
    xml_reader.trim_text(true);
    let mut xml_buf = Vec::new();

    let mut attributes_set: HashSet<String> = HashSet::new();

    let mut stat_attributes = |attributes: Attributes| {
        for attr in attributes.map(|x| x.unwrap()) {
            let name = attr.key.0;
            let name = String::from_utf8_lossy(name).to_string();
            if !attributes_set.contains(&name) {
                attributes_set.insert(name);
            }
        }

        count += 1;
        if count % 1000 == 0 {
            callback(count);
        }
    };

    loop {
        let result = xml_reader.read_event_into(&mut xml_buf);
        match result {
            Ok(Event::Empty(e)) | Ok(Event::Start(e)) => {
                if e.name().as_ref() == b"char" {
                    stat_attributes(e.attributes());
                }
            }
            Ok(Event::Eof) => {
                break;
            }
            Err(e) => {
                panic!("Read XML error: {:?}", e);
            }
            _ => {}
        }
    }

    // the "alias" will not be present in the <char> tag, but as a standalone sub-tag <name-alias>
    // example:
    // <char cp="00AD" age="1.1" na="SOFT HYPHEN" ...>
    //     <name-alias alias="SHY" type="abbreviation"/>
    // <char/>
    assert!(!attributes_set.contains("alias"));
    // manually add the "alias" attribute
    attributes_set.insert(String::from("alias"));
    // make the fields order stable
    let attributes_set = attributes_set.into_iter().collect::<Vec<_>>();
    let mut attr_index_map: HashMap<String, usize> = HashMap::new();
    for (index, attr) in attributes_set.iter().enumerate() {
        attr_index_map.insert(attr.clone(), index);
    }
    Ok((attributes_set, attr_index_map))
}

type OwnedAttributes = Vec<(String, String)>;

pub fn parse_xml<F>(zip_path: &str, sqlite_output: &str, progress_cb: F) -> io::Result<()>
where
    F: Fn(u32, Progress),
{
    let xml_attributes_to_owned = |attributes: Attributes| {
        attributes
            .map(|x| x.unwrap())
            .map(|x| {
                (
                    String::from_utf8(x.key.as_ref().into()).unwrap(),
                    String::from_utf8(x.value.as_ref().into()).unwrap(),
                )
            })
            .collect::<OwnedAttributes>()
    };

    let mut database = Connection::open(sqlite_output).unwrap();

    let attributes_stat =
        stat_attributes(zip_path, |i| progress_cb(i, Progress::StatAttributes)).unwrap();
    // "codepoint" is reserved for the u32 type field, instead of
    // "cp" which is String type.
    assert!(!attributes_stat.0.iter().any(|x| x == "codepoint"));

    let fields = attributes_stat
        .0
        .iter()
        .map(|x| format!(r#""{}" TEXT DEFAULT NULL"#, x))
        .collect::<Vec<_>>()
        .join(", ");
    let create_table_sql = format!(
        "CREATE TABLE IF NOT EXISTS ucd (codepoint INTEGER PRIMARY KEY, json TEXT NOT NULL, {})",
        fields
    );
    database.execute(&create_table_sql, params![]).unwrap();
    let transaction = database.transaction().unwrap();

    let insert_sql = format!(
        r#"INSERT INTO ucd (codepoint, json, {}) VALUES (?, ?, {})"#,
        attributes_stat
            .0
            .iter()
            .map(|x| format!(r#""{x}""#))
            .collect::<Vec<_>>()
            .join(", "),
        (0..attributes_stat.0.len())
            .map(|_| "?")
            .collect::<Vec<_>>()
            .join(", ")
    );
    let mut insert_stmt = transaction.prepare(&insert_sql).unwrap();

    let mut insert_record = |attributes: OwnedAttributes| {
        let mut codepoint = None;

        insert_stmt.clear_bindings();
        for x in &attributes {
            let (key, value) = &x;

            if key == "cp" {
                codepoint = Some(u32::from_str_radix(value, 16).unwrap())
            }

            let index = attributes_stat.1[key]
                +1 /* sqlite index is 1-based*/
                +1 /* skip for the 1st field "codepoint" */
                +1 /* skip for the 2nd field "json" */;

            insert_stmt.raw_bind_parameter(index, value).unwrap();
        }
        insert_stmt
            .raw_bind_parameter(
                1,
                codepoint.unwrap(), /* "cp" field must be present in the attributes */
            )
            .unwrap();
        insert_stmt
            .raw_bind_parameter(2, serde_json::to_string(&attributes).unwrap())
            .unwrap();
        insert_stmt.raw_execute().unwrap();
    };

    let xml_zip = open_zip(zip_path)?;

    let mut xml_reader = quick_xml::Reader::from_reader(BufReader::new(xml_zip));
    xml_reader.trim_text(true);

    let mut buf = Vec::new();
    let mut alias_vec = Vec::new();
    let mut enter_repertoire = false;
    let mut hold_prop = None;
    let mut count = 0_u32;
    loop {
        let event = xml_reader.read_event_into(&mut buf);
        match event {
            Ok(Event::Empty(ref e)) => {
                let name_binary = e.name();
                if enter_repertoire {
                    let first_attr = e.attributes().next().unwrap().unwrap();
                    if name_binary.as_ref() == b"char" && first_attr.key.as_ref() == b"cp" {
                        insert_record(xml_attributes_to_owned(e.attributes()));

                        count += 1;
                        if count % 1000 == 0 {
                            progress_cb(count, Progress::Parse);
                        }
                    } else if name_binary.as_ref() == b"name-alias" {
                        let mut attrs = e.attributes();
                        let alias = attrs
                            .find(|x| x.as_ref().unwrap().key.as_ref() == b"alias")
                            .unwrap()
                            .unwrap();
                        let alias = std::str::from_utf8(alias.value.as_ref()).unwrap();
                        alias_vec.push(String::from(alias));
                    }
                }
            }
            Ok(Event::Start(ref e)) => {
                let name_binary = e.name();
                if enter_repertoire {
                    let first_attr = e.attributes().next().unwrap().unwrap();
                    if name_binary.as_ref() == b"char" && first_attr.key.as_ref() == b"cp" {
                        hold_prop = Some(HoldProp {
                            attributes: xml_attributes_to_owned(e.attributes()),
                        });
                    }
                } else if name_binary.as_ref() == b"repertoire" {
                    enter_repertoire = true;
                }
            }
            Ok(Event::End(ref e)) => {
                if enter_repertoire {
                    match e.name().as_ref() {
                        b"repertoire" => {
                            break;
                        }
                        b"char" => {
                            let mut hold_prop = hold_prop.take().unwrap();

                            let alias_json = serde_json::to_string(&alias_vec).unwrap();

                            hold_prop
                                .attributes
                                .push((String::from("alias"), alias_json));

                            insert_record(hold_prop.attributes);

                            alias_vec.clear();

                            count += 1;
                            if count % 1000 == 0 {
                                progress_cb(count, Progress::Parse);
                            }
                        }
                        _ => {}
                    }
                }
            }
            Ok(Event::Eof) => {
                unreachable!()
            }
            Err(e) => {
                panic!("Reading XML error: {}", e);
            }
            _ => {}
        }
    }

    drop(insert_stmt);
    transaction.commit().unwrap();
    database.close().unwrap();

    Ok(())
}

#[derive(Debug)]
pub enum Progress {
    StatAttributes = 0,
    Parse = 1,
}

struct HoldProp {
    attributes: OwnedAttributes,
}
