package pers.zhc.tools.fourierseries

import kotlin.math.sqrt

/**
 * @author bczhc
 */
class ComplexValue(
    @JvmField val re: Double,
    @JvmField val im: Double
) {
    override fun toString(): String {
        return "ComplexValue(re=$re, im=$im)"
    }

    fun module(): Double {
        return sqrt(re * re + im * im)
    }
}