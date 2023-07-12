use jni::objects::JClass;
use jni::JNIEnv;
use rayon::spawn;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024JniDemo_call2(mut env: JNIEnv, _: JClass) {
    crate::jni_helper::jni_log_global("Log test 0").unwrap();
    spawn(|| {
        crate::jni_helper::jni_log_global("Log test 1").unwrap();
        crate::jni_helper::jni_log_global("Log test 2").unwrap();
        crate::jni_helper::jni_log_global("Log test 3").unwrap();
        crate::jni_helper::jni_log_global("Log test 4").unwrap();
    });
}
