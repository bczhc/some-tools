use std::iter::Peekable;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, jstring};
use jni::JNIEnv;
use unicode_segmentation::UnicodeSegmentation;

use crate::jni_helper::{GetString, GetStringError};

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_newIterator(
    env: JNIEnv,
    _class: JClass,
    text: JString,
) -> jlong {
    let text = match env.get_string_owned(text) {
        Ok(s) => s,
        Err(GetStringError::Utf8Error(e)) => {
            env.throw(format!("UTF-8 error: {}", e)).unwrap();
            "".into()
        }
        Err(GetStringError::JniError(_)) => {
            panic!("JNI error");
        }
    };

    let graphemes = Box::new(Graphemes::new(text));
    Box::into_raw(graphemes) as jlong
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_hasNext(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jboolean {
    unsafe { &mut *(addr as *mut Graphemes) }.has_next() as jboolean
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_next(
    env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jstring {
    let grapheme = unsafe { &mut *(addr as *mut Graphemes) }.next();
    env.new_string(grapheme).unwrap().into_inner()
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Grapheme_release(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) {
    unsafe {
        drop(Box::from_raw(addr as *mut Graphemes));
    }
}

pub struct Graphemes<'a> {
    string: *mut str,
    iter: Peekable<unicode_segmentation::Graphemes<'a>>,
}

impl<'a> Graphemes<'a> {
    pub fn new(string: String) -> Self {
        let raw = Box::into_raw(string.into_boxed_str());
        let graphemes = unsafe { (*raw).graphemes(true).peekable() };

        Self {
            string: raw,
            iter: graphemes,
        }
    }

    pub fn has_next(&mut self) -> bool {
        self.iter.peek().is_some()
    }

    pub fn next(&mut self) -> &str {
        self.iter.next().unwrap()
    }
}

impl<'a> Drop for Graphemes<'a> {
    fn drop(&mut self) {
        unsafe {
            drop(Box::from_raw(self.string));
        }
    }
}
