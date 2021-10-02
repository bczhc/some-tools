package pers.zhc.tools.fourierseries

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
}