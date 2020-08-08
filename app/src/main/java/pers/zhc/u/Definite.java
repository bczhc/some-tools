package pers.zhc.u;

import pers.zhc.u.math.util.MathFunctionInterface;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;

@SuppressWarnings("WeakerAccess")
public class Definite {
    // 0~1区间n等分
    public int n = 100000;

    // 随便定义个曲线e的x次方, 取其x在0~1的定积分;

    // 梯形法求定积分

    /**
     * x0: 坐标下限, xn: 坐标上限
     */
    public double getDefiniteIntegralByTrapezium(double x0, double xn, MathFunctionInterface functionInterface) {
        double h = abs(xn - x0) / n;
        double sum = 0;
        for (double xi = 0; xi <= xn; xi = xi + h) {
            sum += (functionInterface.f(xi) + functionInterface.f(xi + h)) * h / 2;
        }
        return sum;
    }

    /**
     * x0: 坐标下限, xn: 坐标上限
     */
    // 矩形法求定积分, 右边界
    public double getDefiniteIntegralByRectangle1(double x0, double xn, MathFunctionInterface functionInterface) {
        //h: 步长
        double h = abs(xn - x0) / n;
        double sum = 0;
        for (double xi = 0; xi <= xn; xi = xi + h) {
            sum += functionInterface.f(xi + h) * h;
        }
        return sum;
    }

    // 矩形法求定积分, 左边界
    public double getDefiniteIntegralByRectangle2(double x0, double xn, MathFunctionInterface functionInterface) {
        double h = abs(xn - x0) / n;
        double sum = 0;
        for (double xi = 0; xi <= xn; xi = xi + h) {
            sum += functionInterface.f(xi) * h;
        }
        return sum;
    }

    public double getR(double x0, double xn, MathFunctionInterface definiteFunctionInterface) {
        ExecutorService es = Executors.newFixedThreadPool(3);
        final float[] r = new float[3];
        double R;
        CountDownLatch latch = new CountDownLatch(3);
        es.execute(() -> {
            r[0] = (float) getDefiniteIntegralByRectangle1(x0, xn, definiteFunctionInterface);
            latch.countDown();
        });
        es.execute(() -> {
            r[1] = (float) getDefiniteIntegralByRectangle2(x0, xn, definiteFunctionInterface);
            latch.countDown();
        });
        es.execute(() -> {
            r[2] = (float) getDefiniteIntegralByTrapezium(x0, xn, definiteFunctionInterface);
            latch.countDown();
        });
        es.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        R = (r[0] + r[1] + r[2]) / 3;
        return R;
    }
}