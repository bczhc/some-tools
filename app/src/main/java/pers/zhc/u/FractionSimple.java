package pers.zhc.u;//import java.util.Arrays;

public class FractionSimple {
    public static void main(String[] args) {
//        FractionSimple o = new FractionSimple();
//        System.out.println(Arrays.toString(o.Simple(21, 3)));
    }

    long[] FractionSimple(long a, long b) {
        long[] r = new long[2];
        long r_a, r_b, i_ = 0;
        for (long i = 2; i <= Math.min(a, b); i++) {
            if (a % i == 0 && b % i == 0) {
                i_ = i;
            }
        }
        try {
            r_a = a / i_;
            r_b = b / i_;
            r[0] = r_a;
            r[1] = r_b;
        } catch (Exception e) {
            r[0] = a;
            r[1] = b;
        }
        return r;
    }
}