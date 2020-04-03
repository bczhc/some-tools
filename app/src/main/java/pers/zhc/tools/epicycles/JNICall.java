package pers.zhc.tools.epicycles;

import pers.zhc.tools.jni.JNI;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

import java.util.concurrent.CountDownLatch;

/**
 * @author bczhc
 */
public class JNICall implements JNI.FourierSeriesCalc.Callback {
    private final CountDownLatch latch;
    private final ComplexGraphDrawing.SynchronizedPut synchronizedSequence;
    private final ComplexFunctionInterface2 complexFunctionInterface;
    private final ComplexValue result;

    public JNICall(CountDownLatch latch, ComplexGraphDrawing.SynchronizedPut synchronizedSequence, ComplexFunctionInterface2 complexFunctionInterface) {
        this.latch = latch;
        this.synchronizedSequence = synchronizedSequence;
        this.complexFunctionInterface = complexFunctionInterface;
        result = new ComplexValue(0, 0);
    }

    @Override
    public void callback(double n, double re, double im) {
        synchronizedSequence.put(new EpicyclesSequence.Epicycle(n, new ComplexValue(re, im)));
        latch.countDown();
    }

    public void getFunctionResult(double t) {
        complexFunctionInterface.x(result, t);
        JNI.FourierSeriesCalc.nSetFunctionResult(result.re, result.im);
    }
}
