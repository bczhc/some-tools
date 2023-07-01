//! Common compression and decompression utilities.
//!
//! No buffer support.

use std::fs::File;
use std::io;
use std::path::{Path, PathBuf};

use anyhow::anyhow;
use bczhc_lib::io::OpenOrCreate;
use bczhc_lib::str::GenericOsStrExt;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::jint;
use jni::JNIEnv;

use crate::java_str_var;
use crate::jni_helper::CheckOrThrow;

fn progress_callback(
    env: &mut JNIEnv,
    callback: &JObject,
    n: u32,
    total: u32,
    name: &str,
) -> jni::errors::Result<()> {
    env.call_method(
        callback,
        "callback",
        "(IILjava/lang/String;)V",
        &[
            JValue::Int(n as jint),
            JValue::Int(total as jint),
            JValue::Object(&env.new_string(name)?.into()),
        ],
    )?;
    Ok(())
}

fn collect_files<P: AsRef<Path>>(path: P) -> io::Result<Vec<PathBuf>> {
    let walk_dir = walkdir::WalkDir::new(path);
    let mut vec = Vec::new();
    for e in walk_dir {
        let e = e?;
        vec.push(e.into_path());
    }
    Ok(vec)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Compression_createTarZst(
    mut env: JNIEnv,
    _class: JClass,
    dir: JString,
    output: JString,
    level: jint,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        if !(1..=19).contains(&level) {
            Err(anyhow!("Invalid compression level: {}", level))?;
        }

        java_str_var!(env, output, output);
        java_str_var!(env, dir, dir);

        let dir = Path::new(dir).canonicalize()?;
        let mut out_file = File::open_or_create(output)?;

        let mut writer = zstd::Encoder::new(&mut out_file, level)?;
        let mut archive = tar::Builder::new(&mut writer);

        let entries = collect_files(&dir)?;
        for (i, entry) in entries.iter().enumerate() {
            let path = entry.as_path();
            let relative_path = pathdiff::diff_paths(path, &dir).unwrap();
            if relative_path.components().count() == 0 {
                continue;
            }

            let metadata = path.symlink_metadata()?;
            match metadata.file_type() {
                a if a.is_file() => {
                    archive.append_file(&relative_path, &mut File::open(path)?)?;
                }
                a if a.is_dir() => {
                    archive.append_dir(&relative_path, ".")?;
                }
                _ => {
                    // ignore
                }
            }
            progress_callback(
                &mut env,
                &callback,
                i as u32 + 1,
                entries.len() as u32,
                &path.escape(),
            )?;
        }
    };
    result.check_or_throw(&mut env).unwrap();
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Compression_extractTarZst(
    mut env: JNIEnv,
    _class: JClass,
    file: JString,
    output_dir: JString,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        java_str_var!(env, file, file);
        java_str_var!(env, output_dir, output_dir);

        let mut file = File::open(file)?;
        let mut reader = zstd::Decoder::new(&mut file)?;
        let mut archive = tar::Archive::new(&mut reader);
        let entries = archive.entries()?;
        for (i, e) in entries.enumerate() {
            let mut e = e?;
            let result = e.unpack_in(output_dir)?;
            let path = e.path()?;
            if !result {
                env.throw(format!("Skip unpacking {:?}", path.as_ref()))?;
            }
            progress_callback(
                &mut env,
                &callback,
                i as u32 + 1,
                0,
                &path.as_ref().escape(),
            )?;
        }
    };
    result.check_or_throw(&mut env).unwrap();
}
