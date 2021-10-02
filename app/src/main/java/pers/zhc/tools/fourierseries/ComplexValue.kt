package pers.zhc.tools.fourierseries

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
}