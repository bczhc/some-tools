use std::iter::Peekable;
use std::ptr::null;
use std::str::Chars;

use jni::objects::{JClass, JString};
use jni::sys::{_jobject, jboolean, jint, jlong, jstring};
use jni::JNIEnv;

use crate::jni_helper::GetString;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_newIterator(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jlong {
    let s = env.get_string_owned(s);
    if let Err(e) = s {
        env.throw(format!("Invalid string: {:?}", e)).unwrap();
        return 0;
    }
    let s = s.unwrap();
    let iterator = Box::new(StringWithIter::new(s));
    let raw = Box::into_raw(iterator);
    raw as usize as jlong
}

#[inline]
fn get_ref<'a>(addr: jlong) -> &'a mut StringWithIter<'a> {
    let p = addr as usize as *mut StringWithIter;
    unsafe { &mut *p }
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_hasNext(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> jboolean {
    let iter = get_ref(addr);
    iter.chars.peek().is_some() as jboolean
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_next(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) -> i32 {
    let iter = get_ref(addr);
    iter.chars.next().unwrap() as i32
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_release(
    _env: JNIEnv,
    _class: JClass,
    addr: jlong,
) {
    let b = unsafe { Box::from_raw(addr as usize as *mut StringWithIter) };
    drop(b)
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_codepointLength(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> i32 {
    let s = String::from(env.get_string(s).unwrap().to_str().unwrap());
    let mut len = 0_usize;
    for _ in s.chars() {
        len += 1;
    }
    len as jint
}

struct StringWithIter<'a> {
    str: *const String,
    chars: Peekable<Chars<'a>>,
}

impl<'a> Drop for StringWithIter<'a> {
    fn drop(&mut self) {
        unsafe {
            let b = Box::from_raw(self.str as *mut String);
            drop(b);
        }
    }
}

impl<'a> StringWithIter<'a> {
    #[inline]
    fn new(str: String) -> StringWithIter<'a> {
        let raw_ptr = Box::into_raw(Box::new(str));
        Self {
            str: raw_ptr,
            chars: unsafe { (*raw_ptr).chars().peekable() },
        }
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Codepoint_codepoint2str(
    env: JNIEnv,
    _class: JClass,
    codepoint: i32,
) -> jstring {
    let c = std::char::from_u32(codepoint as u32);
    match c {
        None => {
            env.throw("invalid codepoint").unwrap();
            null::<_jobject>() as jstring
        }
        Some(c) => env.new_string(c.to_string()).unwrap().into_inner(),
    }
}
