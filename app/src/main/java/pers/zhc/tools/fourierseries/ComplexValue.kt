package pers.zhc.tools.fourierseries

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * @author bczhc
 */
class ComplexValue(
    @JvmField var re: Double,
    @JvmField var im: Double
) {
    override fun toString(): String {
        return "ComplexValue(re=$re, im=$im)"
    }

    fun module(): Double {
        return sqrt(re * re + im * im)
    }

    operator fun plusAssign(rhs: ComplexValue) {
        re += rhs.re
        im += rhs.im
    }

    operator fun minusAssign(rhs: ComplexValue) {
        re -= rhs.re
        im -= rhs.im
    }

    fun set(re: Double, im: Double) {
        this.re = re
        this.im = im
    }

    fun argument(): Double {
        return atan2(im, re)
    }
}