use std::ffi::OsStr;
use std::fs::File;
use std::io;
use std::io::{Cursor, ErrorKind, Read};
use std::os::unix::prelude::OsStrExt;

use byteorder::{ByteOrder, ReadBytesExt, BE, LE};
use encoding_rs::{GB18030, GBK, UTF_16BE, UTF_16LE, UTF_8};
use jni::objects::{JClass, JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;

use crate::jni_helper::CheckOrThrow;

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Encoding_readFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    encoding: JObject,
) -> jstring {
    let file_content: anyhow::Result<Vec<u8>> = try {
        let str = env.get_string(&path)?;
        let mut file = File::open(OsStr::from_bytes(str.to_bytes()))?;
        let mut buf = Vec::with_capacity(file.metadata()?.len() as usize);
        file.read_to_end(&mut buf)?;
        buf
    };
    file_content.check_or_throw(&mut env).unwrap();
    // FIXME: should return `null` instead of unwrap it and continue the code
    let file_content = file_content.unwrap();

    let n_code = env.get_field(&encoding, "nCode", "I").unwrap().i().unwrap();

    // for UTF32-LE and UTF32-BE
    match n_code {
        3 => {
            // UTF32-LE
            let result = read_utf32::<LE>(&file_content);
            result.check_or_throw(&mut env).unwrap();
            return env.new_string(result.unwrap()).unwrap().into_raw();
        }
        4 => {
            // UTF32-BE
            let result = read_utf32::<BE>(&file_content);
            result.check_or_throw(&mut env).unwrap();
            return env.new_string(result.unwrap()).unwrap().into_raw();
        }
        _ => {}
    }

    let variant = match n_code {
        0 => UTF_8,
        1 => UTF_16LE,
        2 => UTF_16BE,
        3 => unreachable!(),
        4 => unreachable!(),
        5 => GBK,
        6 => GB18030,
        _ => {
            env.throw("Unknown encoding").unwrap();
            return JString::from(JObject::null()).into_raw();
        }
    };

    let result: anyhow::Result<JString> = try {
        let result = variant.decode(&file_content).0;
        env.new_string(&*result)?
    };

    match result {
        Ok(r) => r.into_raw(),
        Err(e) => {
            env.throw(format!("Error: {}", e)).unwrap();
            return JString::from(JObject::null()).into_raw();
        }
    }
}

fn read_utf32<E: ByteOrder>(data: &[u8]) -> io::Result<String> {
    let mut cursor = Cursor::new(data);
    let mut buf = String::new();
    loop {
        let result = cursor.read_u32::<E>();
        match result {
            Ok(r) => {
                if let Some(cp) = char::from_u32(r) {
                    buf.push(cp);
                }
            }
            Err(e) if e.kind() == ErrorKind::UnexpectedEof => {
                break;
            }
            Err(e) => {
                return Err(e);
            }
        }
    }
    Ok(buf)
}
