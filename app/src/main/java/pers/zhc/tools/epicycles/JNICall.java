package pers.zhc.tools.epicycles;

import pers.zhc.tools.jni.JNI;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

/**
 * @author bczhc
 */
public class JNICall implements JNI.FourierSeries.Callback {
    private final ComplexGraphDrawing.SynchronizedPut synchronizedSequence;
    private final ComplexFunctionInterface2 complexFunctionInterface;

    public JNICall(ComplexGraphDrawing.SynchronizedPut synchronizedSequence
            , ComplexFunctionInterface2 complexFunctionInterface) {
        this.synchronizedSequence = synchronizedSequence;
        this.complexFunctionInterface = complexFunctionInterface;
    }

    @Override
    public void callback(double n, double re, double im) {
        synchronizedSequence.put(new EpicyclesSequence.Epicycle(n, new ComplexValue(re, im)));
    }

    public void getFunctionResult(double[] a, double t) {
        ComplexValue result = new ComplexValue(0, 0);
        complexFunctionInterface.x(result, t);
        a[0] = result.re;
        a[1] = result.im;
    }
}
