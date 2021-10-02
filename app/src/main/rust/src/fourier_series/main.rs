use bczhc_lib::complex_num::ComplexValueF64;
use bczhc_lib::fourier_series::{fourier_series_calc, LinearPath};
use bczhc_lib::point::PointF64;
use jni::objects::{GlobalRef, JClass, JObject, JValue};
use jni::sys::{jobject, jobjectArray};
use jni::{JNIEnv, JavaVM};

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_pers_zhc_tools_jni_JNI_00024FourierSeries_compute(
    env: JNIEnv,
    _class: JClass,
    points: jobjectArray,
    integral_segments: i32,
    period: f64,
    epicycle_num: i32,
    thread_num: i32,
    callback: jobject,
) {
    let mut points_vec = Vec::new();

    let points_length = env.get_array_length(points).unwrap();
    for i in 0..points_length {
        let object = env.get_object_array_element(points, i).unwrap();
        let x = env.get_field(object, "x", "F").unwrap().f().unwrap();
        let y = env.get_field(object, "y", "F").unwrap().f().unwrap();
        points_vec.push(PointF64::new(x as f64, y as f64))
    }

    let g_callback = env.new_global_ref(callback).unwrap();
    let g_callback_addr = &g_callback as *const GlobalRef as usize;

    let jvm = env.get_java_vm().unwrap();
    let jvm_addr = &jvm as *const JavaVM as usize;

    let evaluator = LinearPath::new(&points_vec, period);
    let evaluator_addr = &evaluator as *const LinearPath as usize;

    fourier_series_calc(
        epicycle_num as u32,
        period,
        thread_num as u32,
        integral_segments as u32,
        move |t| {
            let evaluator = unsafe { &*(evaluator_addr as *const LinearPath) };
            let p = evaluator.evaluate_path(t);
            ComplexValueF64::new(p.x, p.y)
        },
        move |r| {
            let callback = unsafe { &*(g_callback_addr as *const GlobalRef) };
            let callback = callback.as_obj();

            let jvm = unsafe { &*(jvm_addr as *const JavaVM) };
            let guard = jvm.attach_current_thread().unwrap();
            callback_call(*guard, callback, r.a.re, r.a.im, r.n, r.p).unwrap();
        },
    );
}

fn callback_call(
    env: JNIEnv,
    callback: JObject,
    re: f64,
    im: f64,
    n: i32,
    p: f64,
) -> jni::errors::Result<()> {
    env.call_method(
        callback,
        "onResult",
        "(DDID)V",
        &[
            JValue::Double(re),
            JValue::Double(im),
            JValue::Int(n),
            JValue::Double(p),
        ],
    )?;
    Ok(())
}
