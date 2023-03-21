use jni::objects::{JClass, JString};
use jni::sys::{jobject, jstring};
use jni::JNIEnv;
use unicode_normalization::UnicodeNormalization;

fn transform(mut env: JNIEnv, s: JString, f: fn(&str) -> String) -> jobject {
    let str = env.get_string(&s).unwrap();
    let str = match str.to_str() {
        Ok(s) => s,
        Err(e) => {
            env.throw(format!("Error: {}", e)).unwrap();
            ""
        }
    };
    env.new_string(f(str)).unwrap().into_raw()
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Normalization_nfc(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jstring {
    transform(env, s, |x| x.nfc().collect())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Normalization_nfkc(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jstring {
    transform(env, s, |x| x.nfkc().collect())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Normalization_nfd(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jstring {
    transform(env, s, |x| x.nfd().collect())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Normalization_nfkd(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jstring {
    transform(env, s, |x| x.nfkd().collect())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Unicode_00024Normalization_cjkCompatVariants(
    env: JNIEnv,
    _class: JClass,
    s: JString,
) -> jstring {
    transform(env, s, |x| x.cjk_compat_variants().collect())
}
