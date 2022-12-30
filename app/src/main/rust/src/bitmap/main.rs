use image::{ColorType, ImageFormat};
use jni::objects::{JClass, JString};
use jni::sys::jobject;
use jni::JNIEnv;

use crate::bitmap::error::Result;

fn compress_to_png(env: JNIEnv, bitmap: jobject, dest_path: JString) -> Result<()> {
    let dest_path = env.get_string(dest_path)?;
    let dest_path = dest_path.to_str()?;

    let bitmap =
        unsafe { ndk::bitmap::AndroidBitmap::from_jni(env.get_native_interface(), bitmap) };
    let info = bitmap.get_info()?;

    let size = info.width() as u64 * info.height() as u64 * 4;

    let data = bitmap.lock_pixels()?;
    let data = unsafe { std::slice::from_raw_parts(data as *const u8, size as usize) };

    image::save_buffer_with_format(
        dest_path,
        data,
        info.width(),
        info.height(),
        ColorType::Rgba8,
        ImageFormat::Png,
    )?;

    bitmap.unlock_pixels()?;

    Ok(())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Bitmap_compressToPng(
    env: JNIEnv,
    _class: JClass,
    bitmap: jobject,
    dest_path: JString,
) {
    let result = compress_to_png(env, bitmap, dest_path);
    if let Err(e) = result {
        env.throw(format!("Error: {:?}", e)).unwrap();
    }
}
