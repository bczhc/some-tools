package pers.zhc.u;


import pers.zhc.u.util.FFMap;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("WeakerAccess")
public class FourierSeries {
    public final Definite definite;
    private FFMap mFFMap = null;
    private final double T;
    private final double omega;
    private final double a0;
    private double[] bNC;
    private double[] aNC;
    private final int ffMapI = 0;

    public FourierSeries(double T) {
        this.T = T;
        omega = (Math.PI * 2) / this.T;
        definite = new Definite();
        /*try {
            File f = new File("./FS_result.txt");
            if (!f.exists()) System.out.println("f.createNewFile() = " + f.createNewFile());
            OutputStream os = new FileOutputStream(f, false);
            OutputStreamWriter osw = new OutputStreamWriter(os, "GBK");
            bw = new BufferedWriter(osw);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        a0 = a0();
        /*try {
            bw.write("a0=" + a0 + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public FourierSeries(double T, FFMap ffMap) {
        this.T = T;
        omega = (Math.PI * 2) / this.T;
        definite = new Definite();
        a0 = a0();
        mFFMap = ffMap;
    }

    public static void main(String[] args) {
        FourierSeries fs = new FourierSeries(30D) {
            @Override
            public double f_f(double x) {
                if (x < 10) return x;
                if (x >= 10 && x < 20) return -x + 20;
                if (x >= 20 && x < 25) return x - 20;
                return Math.sin(Math.pow(x, Math.cos(x)));
            }
        };
        fs.definite.n = 10000;
        fs.m();
    }

    public double f_f(double x) {
        return 0;
    }

    public double a0() {
        return (2 / this.T) * definite.getR(0, this.T, this::f_f);
    }

    public double F(int nNum, double x) {
        double sum = 0;
        for (int n = 1; n < nNum; n++) {
            sum += fu(n, x);
//            double nPi = n * Math.PI;
//            double nPPow2 = Math.pow(nPi, 2);
            /*sum += (10D / (nPi) * Math.sin(2D * nPi) + (15D / nPPow2 * Math.cos(2D * nPi)) - 15D / nPPow2) * Math.cos(nPi * x / 15D)
                    + (30D / nPPow2 * Math.sin(2D * nPi / 3D) - 30D / nPPow2 * Math.sin(4D * nPi / 3)
                    - 10D / nPi * Math.cos(2D * nPi) + 15D / nPPow2 * Math.sin(2D * nPi)) * Math.sin(nPi * x / 15D);*/
        }
        return a0 / 2 + sum;
    }

    public void initAB(int nNum, msgInterface msg, int threadNum) {
        CountDownLatch latch = null;
        CountDownLatch latch1 = new CountDownLatch(nNum - 1);
        aNC = new double[nNum - 1];
        bNC = new double[nNum - 1];
        for (int i = 1; i < nNum; i++) {
            if (latch != null && (i - 1) % threadNum == 0) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if ((i - 1) % threadNum == 0) latch = new CountDownLatch(threadNum);
            ExecutorService es = Executors.newFixedThreadPool(threadNum);
            int finalI = i;
            final CountDownLatch finalLatch = latch;
            es.execute(() -> {
                this.aNC[finalI - 1] = aN(finalI);
                this.bNC[finalI - 1] = bN(finalI);
                String s = ((double) finalI - 1) / ((double) nNum) * 100D + "%";
                msg.f(s);
                finalLatch.countDown();
                latch1.countDown();
            });
            es.shutdown();
        }
        try {
            latch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void m() {
        int tN = 16;
        ExecutorService es = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(tN);
        double[] r = new double[tN];
        int j = 0;
        for (double i = 0; i < 20; i += .1) {
            if (i == 20 - .1) return;
            double finalI = i;
            int finalJ = j;
            CountDownLatch finalLatch = latch;
            es.execute(() -> {
                System.out.println("x: " + finalI + "\t" + (r[finalJ % tN] = F(30, finalI)));
                finalLatch.countDown();
            });
            if ((j + 1) % tN == 0 && j != 0) {
                try {
                    latch.await();
                    latch = new CountDownLatch(tN);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("r = " + Arrays.toString(r));
            }
            ++j;
        }
    }

    public double fu(int n, double x) {
        return aNC[n - 1] * Math.cos(((double) n) * this.omega * x) + bNC[n - 1] * Math.sin(((double) n) * this.omega * x);
    }

    public double aN(int n) {
        /*try {
            bw.write("n: " + n + "\ta" + n + "=" + r + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return (2 / this.T) * definite.getDefiniteIntegralByTrapezium(0, this.T, x -> f_f(x) * Math.cos(((double) n) * omega * x));
    }

    public double bN(int n) {
        return (2 / this.T) * definite.getDefiniteIntegralByTrapezium(0, this.T, x -> f_f(x) * Math.sin(((double) n) * this.omega * x));
    }

    public interface msgInterface {
        void f(String s);
    }
}