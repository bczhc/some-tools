use crate::java_str_var;
use crate::jni_helper::{jobject_null, unwrap_or_throw_result};
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use std::fs::File;

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
        let mut vec = Vec::new();
        for x in exif.entries {
            vec.push((
                format!("{}", x.tag),
                format!("{}", x.value),
                format!("{}", x.value_more_readable),
            ));
        }
        vec.sort_by(|a, b| a.0.cmp(&b.0));
        let mut string = String::new();
        for x in vec {
            use std::fmt::Write;
            writeln!(&mut string, "{:?}", x).unwrap();
        }
        let js = env.new_string(string)?;
        js.into_raw()
    };
    unwrap_or_throw_result!(&mut env, result, jobject_null())
}
