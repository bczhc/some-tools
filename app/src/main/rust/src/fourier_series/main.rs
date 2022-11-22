use bczhc_lib::complex::integral;
use bczhc_lib::complex::integral::Integrate;

use bczhc_lib::epicycle::Epicycle;
use bczhc_lib::fourier_series::{compute_iter, EvaluatePath, LinearPath};
use bczhc_lib::point::PointF64;
use jni::objects::{JClass, JValue};
use jni::sys::{jobject, jobjectArray};
use jni::JNIEnv;
use num_complex::Complex64;
use num_traits::FromPrimitive;

use crate::fourier_series::{Integrator, PathEvaluator};

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
    integrator_enum: i32,
    callback: jobject,
) {
    let path_evaluator_enum = PathEvaluator::from_i32(path_evaluator_enum).unwrap();
    let integrator_enum = Integrator::from_i32(integrator_enum).unwrap();

    let mut points_vec = Vec::new();

    let points_length = env.get_array_length(points).unwrap();
    for i in 0..points_length {
        let object = env.get_object_array_element(points, i).unwrap();
        let x = env.get_field(object, "x", "F").unwrap().f().unwrap();
        let y = env.get_field(object, "y", "F").unwrap().f().unwrap();
        points_vec.push(PointF64::new(x as f64, y as f64))
    }

    let _pool = rayon::ThreadPoolBuilder::new()
        .num_threads(thread_num as usize)
        .build()
        .unwrap();

    let thread_num = thread_num as usize;
    // for static dispatching (monomorphization)
    fn compute<E>(integrator: Integrator, params: Params<E>)
    where
        E: EvaluatePath,
    {
        match integrator {
            Integrator::Trapezoid => params.compute::<integral::Trapezoid>(),
            Integrator::LeftRectangle => params.compute::<integral::LeftRectangle>(),
            Integrator::RightRectangle => params.compute::<integral::RightRectangle>(),
            Integrator::Simpson => params.compute::<integral::Simpson>(),
            Integrator::Simpson38 => params.compute::<integral::Simpson38>(),
            Integrator::Boole => params.compute::<integral::Boole>(),
        }
    }
    match path_evaluator_enum {
        PathEvaluator::Linear => {
            let evaluator = LinearPath::new(&points_vec);
            let params = Params {
                env,
                callback,
                epicycle_count: epicycle_num as u32,
                period,
                thread_num,
                integral_segments: integral_segments as u32,
                path_evaluator: evaluator,
            };
            compute(integrator_enum, params);
        }
        PathEvaluator::Time => {
            let evaluator = LinearPath::new(&points_vec);
            let params = Params {
                env,
                callback,
                epicycle_count: epicycle_num as u32,
                period,
                thread_num,
                integral_segments: integral_segments as u32,
                path_evaluator: evaluator,
            };
            compute(integrator_enum, params);
        }
    };
}

struct Params<'a, E>
where
    E: EvaluatePath,
{
    env: JNIEnv<'a>,
    callback: jobject,
    epicycle_count: u32,
    period: f64,
    thread_num: usize,
    integral_segments: u32,
    path_evaluator: E,
}

impl<'a, E> Params<'a, E>
where
    E: EvaluatePath,
{
    fn compute<I>(self)
    where
        I: Integrate + Send,
    {
        let thread_pool = rayon::ThreadPoolBuilder::new()
            .num_threads(self.thread_num)
            .build()
            .unwrap();

        let epicycle_count = self.epicycle_count as i32;
        let n_to = epicycle_count / 2;
        let n_from = -(epicycle_count - n_to) + 1;

        let evaluator_addr = &self.path_evaluator as *const E as usize;
        let period = self.period;
        let epicycles = thread_pool.install(|| {
            compute_iter::<I, _>(n_from, n_to, period, self.integral_segments, move |t| {
                let evaluator = unsafe { &*(evaluator_addr as *const E) };
                let p = evaluator.evaluate(t / period);
                Complex64::new(p.x, p.y)
            })
        });

        for e in epicycles {
            callback_call(self.env, self.callback, e).unwrap();
        }
    }
}

fn callback_call(env: JNIEnv, callback: jobject, epicycle: Epicycle) -> jni::errors::Result<()> {
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
