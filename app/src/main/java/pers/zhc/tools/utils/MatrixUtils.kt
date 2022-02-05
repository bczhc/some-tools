package pers.zhc.tools.utils

import android.graphics.Matrix
import kotlin.reflect.KClass

/**
 * @author bczhc
 */
fun Matrix.getValuesNew(): FloatArray {
    return FloatArray(9).also {
        this.getValues(it)
    }
}

fun KClass<Matrix>.fromValues(values: FloatArray): Matrix {
    return Matrix().apply { setValues(values) }
}