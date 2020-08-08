package pers.zhc.u;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 随机数生成器 by zhc
 * 2018.8.29 0:13
 */
public class Random {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入最小值：");
        int sc_min = sc.nextInt();
        System.out.println("请输入最大值：");
        int sc_max = sc.nextInt();
        System.out.println("请输入组数：");
        int sc_zushu = sc.nextInt();
        System.out.println("请输入生成结果是否重复（随便true或唯一false）：");
        Boolean sc__chong_fu = sc.nextBoolean();
        if (sc_min > sc_max) {
            int tmp = sc_min;
            sc_min = sc_max;
            sc_max = tmp;
        }
        long sc_start = System.currentTimeMillis();
        int[] _r = ran(sc_min, sc_max, sc_zushu, sc__chong_fu);
        long sc_end = System.currentTimeMillis();
        System.out.println(Arrays.toString(_r));
        System.out.println(_r.length + "个数生成完毕！");
        long sc_time = sc_end - sc_start;
        if (sc_time >= 2000) {
            float sc_time_s = (float) sc_time / 1000;
            System.out.println("生成过程共用" + sc_time_s + "s。");
        } else {
            System.out.println("生成过程共用" + sc_time + "ms。");
        }
    }

    /**
     * 生成可调范围随机数
     *
     * @param min 起
     * @param max 止
     * @return n
     */
    public static int ran_sc(int min, int max) {
        double ran_sc_db = Math.round(Math.random() * (max - min)) + min;
        return (int) ran_sc_db;
    }

    private static int[] arrran(int min, int max, int zushu) {
        //    private static int ran_arr[];
        int[] ran_arr = new int[zushu];
        for (int i = 0; i < zushu; i++) {
            ran_arr[i] = ran_sc(min, max);
        }
        return ran_arr;
    }

    public static int[] ran(int min, int max, int zushu, Boolean shifouchongfu_trueOrfalse) {
        int[] r_arr = {};
        if (shifouchongfu_trueOrfalse) {
            r_arr = arrran(min, max, zushu);
        }
        if (!(shifouchongfu_trueOrfalse)) {
            r_arr = arrran_oo(min, max, zushu);
        }
        return r_arr;
    }

    private static int csqian(int x, int[] arr) {
        int r_qian = 0;
        for (int anArr : arr) {
            if (x == anArr) {
                r_qian = 1;
                break;
            }
        }
        return r_qian;
    }

    private static int[] s_c(int min, int max, int zushu) {
        int[] arr = new int[zushu];
        if ((min * max < 0) || (min == 0 || max == 0)) {
            for (int f = 0; f < zushu; f++) {
                arr[f] = max + 1;
            }
        }
        for (int m = 0; m > -1; m++) {
            if (m == zushu) break;
            int random_oo = ran_sc(min, max);
            if (csqian(random_oo, arr) == 0) {
                arr[m] = random_oo;
            } else if (csqian(random_oo, arr) == 1) {
                m -= 1;
            }
        }
        return arr;
    }

    private static int[] arrran_oo(int min, int max, int zushu) {
        int[] r_ = new int[zushu];
        if (zushu > (max - min + 1)) {
            System.out.println("组数应不超过能生成的所有不一样的随机数的数目\n（即组数不超过(最大值-最小值+1)）");
            System.exit(0);
        } else {
            r_ = s_c(min, max, zushu);
        }
        return r_;
    }
}