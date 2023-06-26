//! Common compression and decompression utilities.
//!
//! No buffer support.

use anyhow::anyhow;
use std::fs::{File, FileType};
use std::io;
use std::path::{Path, PathBuf};

use bczhc_lib::io::OpenOrCreate;
use bytesize::KIB;
use jni::objects::{JClass, JString};
use jni::JNIEnv;

use crate::java_str_var;
use crate::jni_helper::{jni_log, CheckOrThrow};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Compression_createTarBz3(
    mut env: JNIEnv,
    _class: JClass,
    dir: JString,
    output: JString,
) {
    let result: anyhow::Result<()> = try {
        java_str_var!(env, output, output);
        java_str_var!(env, dir, dir);

        let dir = Path::new(dir).canonicalize()?;
        let mut out_file = File::open_or_create(output)?;

        let mut writer = bzip3::write::Bz3Encoder::new(&mut out_file, 1024 * KIB as usize)?;
        let mut archive = tar::Builder::new(&mut writer);

        let walk_dir = walkdir::WalkDir::new(&dir);
        for e in walk_dir {
            let entry = e?;
            let path = entry.path().canonicalize()?;
            let relative_path = pathdiff::diff_paths(&path, &dir).unwrap();
            if relative_path.components().count() == 0 {
                continue;
            }

            let metadata = path.symlink_metadata()?;
            match metadata.file_type() {
                a if a.is_file() => {
                    archive.append_file(&relative_path, &mut File::open(&path)?)?;
                }
                a if a.is_dir() => {
                    archive.append_dir(&relative_path, ".")?;
                }
                _ => {
                    // ignore
                }
            }
        }
    };
    result.check_or_throw(&mut env).unwrap();
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Compression_extractTarBz3(
    mut env: JNIEnv,
    _class: JClass,
    file: JString,
    output_dir: JString,
) {
    let result: anyhow::Result<()> = try {
        java_str_var!(env, file, file);
        java_str_var!(env, output_dir, output_dir);

        let mut file = File::open(file)?;
        let mut reader = bzip3::read::Bz3Decoder::new(&mut file)?;
        let mut archive = tar::Archive::new(&mut reader);
        let entries = archive.entries()?;
        for e in entries {
            let mut e = e?;
            let path = e.path()?;
            let output_path = PathBuf::from(output_dir).join(path);
            let mut output_file = File::open_or_create(output_path)?;
            io::copy(&mut e, &mut output_file)?;
        }
    };
    result.check_or_throw(&mut env).unwrap();
}
