use std::sync::Mutex;

use bczhc_lib::complex_num::ComplexValueF64;
use bczhc_lib::epicycle::Epicycle;
use bczhc_lib::fourier_series::{EvaluatePath, LinearPath, TimePath};
use bczhc_lib::point::PointF64;
use bczhc_lib::{fourier_series, mutex_lock};
use jni::objects::{GlobalRef, JClass, JObject, JValue};
use jni::sys::{jobject, jobjectArray};
use jni::{JNIEnv, JavaVM};
use once_cell::sync::Lazy;
use rayon::prelude::*;

static JAVA_VM: Lazy<Mutex<Option<JavaVM>>> = Lazy::new(|| Mutex::new(None));
static RESULT_CALLBACK: Lazy<Mutex<Option<GlobalRef>>> = Lazy::new(|| Mutex::new(None));

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub fn Java_pers_zhc_tools_jni_JNI_00024FourierSeries_compute(
    env: JNIEnv,
    _class: JClass,
    points: jobjectArray,
    integral_segments: i32,
    period: f64,
    epicycle_num: i32,
    thread_num: i32,
    path_evaluator_enum: i32,
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

    let global_callback = env.new_global_ref(callback).unwrap();
    mutex_lock!(RESULT_CALLBACK).replace(global_callback);

    let jvm = env.get_java_vm().unwrap();
    mutex_lock!(JAVA_VM).replace(jvm);

    let pool = rayon::ThreadPoolBuilder::new()
        .num_threads(thread_num as usize)
        .build()
        .unwrap();

    let path_evaluator_enum = PathEvaluator::from(path_evaluator_enum).unwrap();
    // for static dispatching (monomorphization)
    let compute_fn = || match path_evaluator_enum {
        PathEvaluator::Linear => {
            let evaluator = LinearPath::new(&points_vec);
            compute(
                epicycle_num as u32,
                period,
                integral_segments as u32,
                evaluator,
            );
        }
        PathEvaluator::Time => {
            let evaluator = TimePath::new(&points_vec);
            compute(
                epicycle_num as u32,
                period,
                integral_segments as u32,
                evaluator,
            );
        }
    };
    pool.install(compute_fn);
}

enum PathEvaluator {
    Linear,
    Time,
}

impl PathEvaluator {
    fn from(enum_int: i32) -> Option<Self> {
        match enum_int {
            0 => Some(PathEvaluator::Linear),
            1 => Some(PathEvaluator::Time),
            _ => None,
        }
    }
}

fn compute<T>(
    epicycle_count: u32,
    period: f64,
    // thread_num: u32,
    integral_segments: u32,
    path_evaluator: T,
) where
    T: EvaluatePath,
{
    let epicycle_count = epicycle_count as i32;
    let n_to = epicycle_count / 2;
    let n_from = -(epicycle_count - n_to) + 1;

    let evaluator_addr = &path_evaluator as *const T as usize;

    (n_from..=n_to)
        .into_par_iter()
        .map(move |n| {
            fourier_series::calc_n(period, integral_segments, n, move |t| {
                let evaluator = unsafe { &*(evaluator_addr as *const T) };
                let p = evaluator.evaluate(t / period);
                ComplexValueF64::new(p.x, p.y)
            })
        })
        .for_each(|epicycle| {
            let jvm_guard = mutex_lock!(JAVA_VM);
            let callback_guard = mutex_lock!(RESULT_CALLBACK);

            let jvm = jvm_guard.as_ref();
            let jvm = jvm.unwrap();

            let callback = callback_guard.as_ref();
            let callback = callback.unwrap().as_obj();

            let attached = jvm.attach_current_thread().unwrap();
            let env = *attached;
            callback_call(env, callback, epicycle).unwrap();
            drop(attached);

            drop(callback_guard);
            drop(jvm_guard);
        });
}

fn callback_call(env: JNIEnv, callback: JObject, epicycle: Epicycle) -> jni::errors::Result<()> {
    env.call_method(
        callback,
        "onResult",
        "(DDID)V",
        &[
            JValue::Double(epicycle.a.re),
            JValue::Double(epicycle.a.im),
            JValue::Int(epicycle.n),
            JValue::Double(epicycle.p),
        ],
    )?;
    Ok(())
}
