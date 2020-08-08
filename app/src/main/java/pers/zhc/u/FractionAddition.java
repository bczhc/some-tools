package pers.zhc.u;

import java.util.Scanner;

public class FractionAddition {
    private static final FractionAddition o = new FractionAddition();
    private static final FractionSimple o1 = new FractionSimple();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("输入第一个分数分子");
        long a = sc.nextLong();
        System.out.println("输入第一个分数分母");
        long b = sc.nextLong();
        System.out.println("输入第二个分数分子");
        long c = sc.nextLong();
        System.out.println("输入第二个分数分母");
        long d = sc.nextLong();
        System.out.println(o.FractionAdd(a, b, c, d));
        System.out.println("------------------------------");
        main(args);
    }

    private String FractionAdd(long a, long b, long c, long d) {
        long a_, c_, ab_multiple, cd_multiple, GCD = o.getLCM(b, d), r_a, r_b;
        ab_multiple = GCD / b;
        cd_multiple = GCD / d;
        a_ = a * ab_multiple;
        c_ = c * cd_multiple;
        r_a = a_ + c_;
        r_b = GCD;
        //化简分数
        long[] fractionSimple = o1.FractionSimple(r_a, r_b);
        r_a = fractionSimple[0];
        r_b = fractionSimple[1];
        return r_a + " / " + r_b;
    }

    private long getLCM(long a, long b) {
        long r = 0;
        for (long i = Math.min(a, b); i <= a * b; i++) {
            if (i % a == 0 && i % b == 0) {
                r = i;
                break;
            }
        }
        return r;
    }
}