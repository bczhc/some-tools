//! Common compression and decompression utilities.
//!
//! No buffer support.

use std::fs::File;
use std::io;
use std::io::{Read, Write};
use std::path::{Path, PathBuf};

use anyhow::anyhow;
use bczhc_lib::io::OpenOrCreate;
use bczhc_lib::str::GenericOsStrExt;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::jint;
use jni::JNIEnv;

use crate::java_str_var;
use crate::jni_helper::CheckOrThrow;

pub fn progress_callback(
    env: &mut JNIEnv,
    callback: &JObject,
    n: u32,
    total: u32,
    name: &String,
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

pub fn collect_files<P: AsRef<Path>>(path: P) -> io::Result<Vec<PathBuf>> {
    let walk_dir = walkdir::WalkDir::new(path);
    let mut vec = Vec::new();
    for e in walk_dir {
        let e = e?;
        vec.push(e.into_path());
    }
    Ok(vec)
}

/// Returns the count of entries
pub fn write_tar<P, W, F>(dir: P, writer: W, mut progress_callback: F) -> io::Result<usize>
where
    P: AsRef<Path>,
    W: Write,
    F: FnMut(usize, usize, &Path),
{
    let entries = collect_files(&dir)?;
    let mut archive = tar::Builder::new(writer);

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
        progress_callback(i + 1, entries.len(), path);
    }
    archive.finish()?;
    Ok(entries.len())
}

/// Returns the count of entries
pub fn write_tar_zst<P, W, F>(
    dir: P,
    writer: W,
    compression_level: u32,
    progress_callback: F,
) -> io::Result<usize>
where
    P: AsRef<Path>,
    W: Write,
    F: FnMut(usize, usize, &Path),
{
    let mut encoder = zstd::Encoder::new(writer, compression_level as i32)?;
    let count = write_tar(dir, &mut encoder, progress_callback)?;
    encoder.finish()?;
    Ok(count)
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

        let mut encoder = zstd::Encoder::new(&mut out_file, level)?;

        write_tar(&dir, &mut encoder, |n, total, p| {
            progress_callback(&mut env, &callback, n as u32, total as u32, &p.escape()).unwrap();
        })?;
        encoder.finish()?;
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

        let mut i = 0_u32;
        extract_tar_zst(&mut reader, output_dir, |name| {
            i += 1;
            progress_callback(&mut env, &callback, i, 0, &name.escape()).unwrap();
        })?;
    };
    result.check_or_throw(&mut env).unwrap();
}

pub fn extract_tar_zst<R, P, F>(reader: R, out_dir: P, mut progress_callback: F) -> io::Result<()>
where
    P: AsRef<Path>,
    F: FnMut(&Path),
    R: Read,
{
    let mut decoder = zstd::Decoder::new(reader)?;
    let mut archive = tar::Archive::new(&mut decoder);
    let entries = archive.entries()?;
    for e in entries {
        let mut e = e?;
        let _result = e.unpack_in(&out_dir)?;
        let path = e.path()?;
        progress_callback(path.as_ref());
    }
    Ok(())
}
