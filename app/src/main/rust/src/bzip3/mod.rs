use std::io::Cursor;
use std::{io, slice};

use jni::objects::{JByteArray, JClass, ReleaseMode};
use jni::sys::{jbyteArray, jlong, jstring};
use jni::JNIEnv;

fn compress_bytes(data: &[u8], block_size: usize) -> bzip3::errors::Result<Vec<u8>> {
    let mut reader = Cursor::new(data);
    let mut encoder = bzip3::read::Bz3Encoder::new(&mut reader, block_size)?;
    let mut writer = Cursor::new(Vec::new());
    io::copy(&mut encoder, &mut writer).unwrap();
    Ok(writer.into_inner())
}

fn decompress_bytes(data: &[u8]) -> bzip3::errors::Result<Vec<u8>> {
    let mut reader = Cursor::new(data);
    let mut writer = Cursor::new(Vec::new());
    let mut decoder = bzip3::read::Bz3Decoder::new(&mut reader)?;
    io::copy(&mut decoder, &mut writer).unwrap();
    Ok(writer.into_inner())
}

enum ProcessType {
    Compress,
    Decompress,
}
fn jni_process_bytes(
    env: &mut JNIEnv,
    data: &JByteArray,
    block_size: Option<usize>,
    r#type: ProcessType,
) -> jbyteArray {
    unsafe {
        let result = env
            .get_array_elements(&data, ReleaseMode::NoCopyBack)
            .unwrap();
        let result = match r#type {
            ProcessType::Compress => compress_bytes(
                slice::from_raw_parts(result.as_ptr() as *const u8, result.len()),
                block_size.expect("Block size required"),
            ),
            ProcessType::Decompress => decompress_bytes(slice::from_raw_parts(
                result.as_ptr() as *const u8,
                result.len(),
            )),
        };
        match result {
            Ok(d) => env.byte_array_from_slice(&d).unwrap().into_raw(),
            Err(e) => {
                env.throw(format!("Error: {}", e)).unwrap();
                env.new_byte_array(0).unwrap().into_raw()
            }
        }
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024BZip3_compress(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
    block_size: jlong,
) -> jbyteArray {
    jni_process_bytes(
        &mut env,
        &data,
        Some(block_size as usize),
        ProcessType::Compress,
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024BZip3_decompress(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jbyteArray {
    jni_process_bytes(&mut env, &data, None, ProcessType::Decompress)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024BZip3_version(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    env.new_string(bzip3::version()).unwrap().into_raw()
}
