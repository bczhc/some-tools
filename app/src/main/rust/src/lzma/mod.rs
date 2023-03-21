use jni::objects::{JByteArray, JClass};
use jni::sys::jbyteArray;
use jni::JNIEnv;
use std::io::Cursor;
mod errors;
use errors::*;

fn decompress<'a>(env: &mut JNIEnv<'a>, data: &JByteArray) -> Result<JByteArray<'a>> {
    let mut input_cursor = Cursor::new(env.convert_byte_array(data)?);
    let mut output_cursor = Cursor::new(Vec::new());

    lzma_rs::lzma_decompress(&mut input_cursor, &mut output_cursor)?;

    Ok(env.byte_array_from_slice(&output_cursor.into_inner())?)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Lzma_decompress(
    mut env: JNIEnv,
    _: JClass,
    data: JByteArray,
) -> jbyteArray {
    let result = decompress(&mut env, &data);
    if let Err(ref e) = result {
        env.throw(format!("{:?}", e)).unwrap();
    }
    result.unwrap().into_raw()
}
