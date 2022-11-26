mod main;

pub use main::Java_pers_zhc_tools_jni_JNI_00024FourierSeries_compute;

use num_derive::FromPrimitive;

#[derive(FromPrimitive)]
enum PathEvaluator {
    Linear = 0,
    Time = 1,
}

#[derive(FromPrimitive)]
enum Integrator {
    Trapezoid = 0,
    LeftRectangle = 1,
    RightRectangle = 2,
    Simpson = 3,
    Simpson38 = 4,
    Boole = 5,
}

#[derive(FromPrimitive)]
enum FloatType {
    F32,
    F64,
}
