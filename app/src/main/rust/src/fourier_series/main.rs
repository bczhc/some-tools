use bczhc_lib::complex::integral;
use bczhc_lib::complex::integral::Integrate;
use bczhc_lib::epicycle::Epicycle;
use bczhc_lib::fourier_series::{compute_iter, euclid, EvaluatePath, LinearPath, TimePath};
use jni::objects::{JClass, JObject, JObjectArray, JValue};
use jni::sys::jfloat;
use jni::JNIEnv;
use num_complex::Complex;
use num_traits::{AsPrimitive, Float, FromPrimitive, NumAssign};

use crate::fourier_series::{FloatType, Integrator, PathEvaluator};

type Point<T> = euclid::Point2D<T, ()>;

#[no_mangle]
#[allow(non_snake_case, clippy::too_many_arguments)]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024FourierSeries_compute(
    env: JNIEnv,
    _class: JClass,
    points: JObjectArray,
    integral_segments: i32,
    period: f64,
    epicycle_num: i32,
    thread_num: i32,
    path_evaluator_enum: i32,
    integrator_enum: i32,
    float_type_enum: i32,
    callback: JObject,
) {
    let path_evaluator_enum = PathEvaluator::from_i32(path_evaluator_enum).unwrap();
    let integrator_enum = Integrator::from_i32(integrator_enum).unwrap();
    let float_type = FloatType::from_i32(float_type_enum).unwrap();

    match float_type {
        FloatType::F32 => compute_with_float_type::<f32>(
            env,
            points,
            integral_segments,
            period as f32,
            epicycle_num,
            thread_num,
            path_evaluator_enum,
            integrator_enum,
            callback,
        ),
        FloatType::F64 => compute_with_float_type::<f64>(
            env,
            points,
            integral_segments,
            period,
            epicycle_num,
            thread_num,
            path_evaluator_enum,
            integrator_enum,
            callback,
        ),
    }
}

#[allow(clippy::too_many_arguments)]
fn compute_with_float_type<F>(
    mut env: JNIEnv,
    points: JObjectArray,
    integral_segments: i32,
    period: F,
    epicycle_num: i32,
    thread_num: i32,
    path_evaluator_enum: PathEvaluator,
    integrator_enum: Integrator,
    callback: JObject,
) where
    F: bczhc_lib::fourier_series::FloatNum + NumAssign + Send + Sync + 'static,
    jfloat: AsPrimitive<F>,
    usize: AsPrimitive<F>,
    i32: AsPrimitive<F>,
    u32: AsPrimitive<F>,
{
    let mut points_vec: Vec<Point<F>> = Vec::new();

    let points_length = env.get_array_length(&points).unwrap();
    for i in 0..points_length {
        let object = env.get_object_array_element(&points, i).unwrap();
        let x = env.get_field(&object, "x", "F").unwrap().f().unwrap();
        let y = env.get_field(&object, "y", "F").unwrap().f().unwrap();
        points_vec.push(Point::new(x.as_(), y.as_()))
    }

    let _pool = rayon::ThreadPoolBuilder::new()
        .num_threads(thread_num as usize)
        .build()
        .unwrap();

    let thread_num = thread_num as usize;
    // for static dispatching (monomorphization)
    fn compute<E, F>(integrator: Integrator, params: Params<E, F>)
    where
        E: EvaluatePath<F>,
        F: bczhc_lib::fourier_series::FloatNum + NumAssign + Send + Sync + 'static,
        i32: AsPrimitive<F>,
        u32: AsPrimitive<F>,
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
            let evaluator = TimePath::new(&points_vec);
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

struct Params<'a, E, F>
where
    E: EvaluatePath<F>,
    F: bczhc_lib::fourier_series::FloatNum + NumAssign + Send + Sync + 'static,
    i32: AsPrimitive<F>,
    u32: AsPrimitive<F>,
{
    env: JNIEnv<'a>,
    callback: JObject<'a>,
    epicycle_count: u32,
    period: F,
    thread_num: usize,
    integral_segments: u32,
    path_evaluator: E,
}

impl<'a, E, F> Params<'a, E, F>
where
    E: EvaluatePath<F>,
    F: bczhc_lib::fourier_series::FloatNum + NumAssign + Send + Sync + 'static,
    i32: AsPrimitive<F>,
    u32: AsPrimitive<F>,
{
    fn compute<I>(mut self)
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
            compute_iter::<I, _, F>(n_from, n_to, period, self.integral_segments, move |t| {
                let evaluator = unsafe { &*(evaluator_addr as *const E) };
                let p = evaluator.evaluate(t / period);
                Complex::new(p.x, p.y)
            })
        });

        for e in epicycles {
            callback_call(&mut self.env, &self.callback, e).unwrap();
        }
    }
}

fn callback_call<F>(
    env: &mut JNIEnv,
    callback: &JObject,
    epicycle: Epicycle<F>,
) -> jni::errors::Result<()>
where
    F: Float,
    F: AsPrimitive<f64>,
{
    env.call_method(
        callback,
        "onResult",
        "(DDID)V",
        &[
            JValue::Double(epicycle.a.re.as_()),
            JValue::Double(epicycle.a.im.as_()),
            JValue::Int(epicycle.n),
            JValue::Double(epicycle.p.as_()),
        ],
    )?;
    Ok(())
}
