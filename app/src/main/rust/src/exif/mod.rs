use crate::java_str_var;
use crate::jni_helper::{jobject_null, unwrap_or_throw_result};
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde::Serialize;
use std::fs::File;

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ExifEntry {
    pub tag: String,
    pub tag_desc: String,
    pub value_display: String,
    pub value_readable: String,
    pub value_internal: String,
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Exif_getExifInfo(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jstring {
    let result: anyhow::Result<jstring> = try {
        java_str_var!(env, path, path);
        let exif = rexif::read_file(&mut File::open(path)?)?;
        let mut entries = Vec::new();
        for x in exif.entries {
            let entry = ExifEntry {
                tag: format!("{:?}", x.tag),
                tag_desc: format!("{}", x.tag),
                value_internal: format!("{:?}", x.value),
                value_readable: format!("{}", x.value_more_readable),
                value_display: format!("{}", x.value),
            };
            entries.push(entry);
        }
        entries.sort_by(|a, b| a.tag_desc.cmp(&b.tag_desc));
        let json_string = serde_json::to_string(&entries).unwrap();
        env.new_string(json_string)?.into_raw()
    };
    unwrap_or_throw_result!(&mut env, result, jobject_null())
}
