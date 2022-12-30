use std::fs::File;
use std::io;
use std::path::Path;

use byteorder::{WriteBytesExt, LE};
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use sha1::digest::Digest;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Diary_computeFileIdentifier(
    env: JNIEnv,
    _: JClass,
    path: JString,
) -> jstring {
    let path = env.get_string(path).unwrap();
    // TODO: handle filenames with non-UTF8-encoded bytes which
    // is totally valid in some filesystems
    let result = compute_identifier(path.to_str().unwrap());
    match result {
        Ok(hash) => {
            let hex_string = hex::encode(hash);
            env.new_string(hex_string).unwrap().into_inner()
        }
        Err(e) => {
            env.throw(format!("{:?}", e)).unwrap();
            env.new_string("").unwrap().into_inner()
        }
    }
}

fn compute_identifier<P: AsRef<Path>>(path: P) -> io::Result<[u8; 20]> {
    let file_size = path.as_ref().metadata()?.len();

    let mut hasher = sha1::Sha1::new();

    let mut file = File::open(path)?;
    io::copy(&mut file, &mut hasher)?;
    hasher.write_u64::<LE>(file_size)?;

    Ok(hasher.finalize().into())
}
