package pers.zhc.tools.epicycles;

import pers.zhc.u.math.util.ComplexValue;

/**
 * @author bczhc
 */
public interface ComplexFunctionInterface2 {
    /**
     * function x(t): ComplexValue
     * @param dest destination
     * @param t param t
     */
    void x(ComplexValue dest, double t);
}
