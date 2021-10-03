package pers.zhc.tools.fourierseries

import kotlin.math.cos
import kotlin.math.sin

/**
 * @author bczhc
 */
class Epicycle(
    @JvmField val n: Int,
    @JvmField val an: ComplexValue,
    @JvmField val p: Double
) {
    override fun toString(): String {
        return "Epicycle(n=$n, an=$an, p=$p)"
    }

    fun radius(): Double {
        return an.module()
    }

    fun evaluate(t: Double, dest: ComplexValue) {
        val s = sin(p * t)
        val c = cos(p * t)
        dest.set(an.re * c - an.im * s, an.re * s + an.im * c)
    }
}