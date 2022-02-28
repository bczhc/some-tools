use std::fs::File;
use std::io::{BufReader, BufWriter, Write};

use bczhc_lib::io::OpenOrCreate;
use quick_xml::events::attributes::Attributes;
use quick_xml::events::Event;
use quick_xml::Reader;
use serde_json::Value;

fn get_xml_reader(path: &str) -> Reader<BufReader<File>> {
    let file = File::open(path).unwrap();
    let reader = BufReader::new(file);

    let mut xml_reader = Reader::from_reader(reader);
    xml_reader.trim_text(true);
    xml_reader
}

pub fn read_total_count<F>(path: &str, callback: F) -> u32
    where
        F: Fn(u32),
{
    let mut total = 0_u32;

    let mut xml_reader = get_xml_reader(path);

    let mut buf = Vec::new();
    let mut repertoire_enter = false;
    loop {
        let event = xml_reader.read_event(&mut buf);
        match event {
            Ok(Event::Start(ref e)) | Ok(Event::Empty(ref e)) => {
                let name_binary = e.name();
                if repertoire_enter {
                    if name_binary == b"char"
                        && e.attributes().next().unwrap().unwrap().key == b"cp"
                    {
                        total += 1;
                        callback(total);
                    }
                } else if name_binary == b"repertoire" {
                    repertoire_enter = true;
                }
            }
            Ok(Event::End(ref e)) => if repertoire_enter && e.name() == b"repertoire" {
                break;
            },
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
    total
}

fn attributes2json(attrs: Attributes) -> Value {
    serde_json::Value::Array(
        attrs
            .map(|x| {
                let x = x.unwrap();
                let mut m = serde_json::Map::new();
                m.insert(
                    String::from_utf8_lossy(x.key).to_string(),
                    serde_json::Value::String(String::from_utf8_lossy(&*x.value).to_string()),
                );
                serde_json::value::Value::Object(m)
            })
            .collect(),
    )
}

fn to_output_line(codepoint: u32, attrs_json: Value) -> String {
    format!("{} {}", codepoint, attrs_json)
}

struct HoldProp {
    codepoint: u32,
    json: Value,
}

// TODO: handle some `unwrap` (e.g. IO `Error`) and throw it to Java7
pub fn write_intermediate<F>(xml_path: &str, output_path: &str, callback: F)
    where
        F: Fn(i32),
{
    let mut xml_reader = get_xml_reader(xml_path);
    let mut output_writer = BufWriter::new(File::open_or_create(output_path).unwrap());

    let mut buf = Vec::new();
    let mut alias_vec = Vec::new();
    let mut enter_repertoire = false;
    let mut hold_prop = None;
    let mut count = 0;
    loop {
        let event = xml_reader.read_event(&mut buf);
        match event {
            Ok(Event::Empty(ref e)) => {
                let name_binary = e.name();
                if enter_repertoire {
                    let first_attr = e.attributes().next().unwrap().unwrap();
                    if name_binary == b"char" && first_attr.key == b"cp" {
                        let codepoint = u32::from_str_radix(
                            std::str::from_utf8(first_attr.value.as_ref()).unwrap(),
                            16,
                        )
                            .unwrap();
                        let attr_json = attributes2json(e.attributes());
                        let output_line = to_output_line(codepoint, attr_json);

                        output_writer.write_all(output_line.as_bytes()).unwrap();
                        output_writer.write_all(b"\n").unwrap();

                        count += 1;
                        if count % 1000 == 0 {
                            callback(count);
                        }
                    } else if name_binary == b"name-alias" {
                        let mut attrs = e.attributes();
                        let alias = attrs
                            .find(|x| x.as_ref().unwrap().key == b"alias")
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
                    if name_binary == b"char" && first_attr.key == b"cp" {
                        let codepoint = u32::from_str_radix(
                            std::str::from_utf8(first_attr.value.as_ref()).unwrap(),
                            16,
                        )
                            .unwrap();

                        let attr_json = attributes2json(e.attributes());

                        hold_prop = Some(HoldProp {
                            codepoint,
                            json: attr_json,
                        });
                    }
                } else if name_binary == b"repertoire" {
                    enter_repertoire = true;
                }
            }
            Ok(Event::End(ref e)) => {
                if enter_repertoire {
                    match e.name() {
                        b"repertoire" => {
                            break;
                        }
                        b"char" => {
                            let mut hold_prop = hold_prop.take().unwrap();

                            let alias_json = Value::Object({
                                let mut obj_map = serde_json::Map::new();
                                obj_map.insert(
                                    String::from("alias"),
                                    Value::Array(
                                        alias_vec
                                            .iter()
                                            .map(|x| Value::String(x.clone()))
                                            .collect(),
                                    ),
                                );
                                obj_map
                            });
                            hold_prop.json.as_array_mut().unwrap().push(alias_json);

                            let output_line = to_output_line(hold_prop.codepoint, hold_prop.json);
                            output_writer.write_all(output_line.as_bytes()).unwrap();
                            output_writer.write_all(b"\n").unwrap();

                            alias_vec.clear();

                            count += 1;
                            if count % 1000 == 0 {
                                callback(count);
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
}
