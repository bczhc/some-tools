use std::fs::File;
use std::io::{Read, Seek, SeekFrom, Write};

use bczhc_lib::io::OpenOrCreate;
use bczhc_lib::str::GenericOsStrExt;
use byteorder::{ReadBytesExt, WriteBytesExt, LE};
use jni::objects::{JClass, JObject, JString};
use jni::sys::jint;
use jni::JNIEnv;

use crate::compression::{extract_tar_zst, write_tar_zst};
use crate::java_str_var;
use crate::jni_helper::CheckOrThrow;

/// Format: \[content1_length (8), total_entry_count (8), content1, content2\]
#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024App_archiveAppData(
    mut env: JNIEnv,
    _: JClass,
    output: JString,
    s1: JString,
    s2: JString,
    compression_level: jint,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        java_str_var!(env, internal_files_dir, s1);
        java_str_var!(env, external_files_dir, s2);
        java_str_var!(env, output, output);

        let mut out_file = File::open_or_create(output)?;
        out_file.write_all(&[0_u8; 2 * 8])? /* header info placeholder */;

        macro_rules! write_archive {
            ($dir:expr) => {
                write_tar_zst(
                    $dir,
                    &mut out_file,
                    compression_level as u32,
                    |n, total, name| {
                        crate::compression::progress_callback(
                            &mut env,
                            &callback,
                            n as u32,
                            total as u32,
                            &name.escape(),
                        )
                        .unwrap();
                    },
                )?
            };
        }

        // internal files
        let content1_entry_count = write_archive!(internal_files_dir);

        let content2_offset = out_file.stream_position()?;
        // external files
        let content2_entry_count = write_archive!(external_files_dir);

        out_file.seek(SeekFrom::Start(0))?;
        out_file.write_u64::<LE>(content2_offset - 2 * 8)?;
        out_file.write_u64::<LE>((content1_entry_count + content2_entry_count) as u64)?;
    };
    result.check_or_throw(&mut env).unwrap();
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024App_extractAppData(
    mut env: JNIEnv,
    _: JClass,
    file: JString,
    s1: JString,
    s2: JString,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        java_str_var!(env, internal_files_dir, s1);
        java_str_var!(env, external_files_dir, s2);
        java_str_var!(env, file, file);

        let mut counter = 0;

        let mut file = File::open(file)?;
        let content1_length = file.read_u64::<LE>()?;
        let all_entry_count = file.read_u64::<LE>()?;

        macro_rules! extract {
            ($reader:expr, $dest_dir:expr) => {
                extract_tar_zst($reader, $dest_dir, |n| {
                    counter += 1;
                    crate::compression::progress_callback(
                        &mut env,
                        &callback,
                        counter,
                        all_entry_count as u32,
                        &n.escape(),
                    )
                    .unwrap();
                })?;
            };
        }
        let mut content1 = file.try_clone()?.take(content1_length);
        extract!(&mut content1, internal_files_dir);

        file.seek(SeekFrom::Start(content1_length + 2 * 8))?;
        extract!(&mut file, external_files_dir);
    };
    result.check_or_throw(&mut env).unwrap();
}
