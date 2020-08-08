package pers.zhc.u;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IsPrime {
    @SuppressWarnings("InfiniteRecursion")
    public static void main(String[] args) {
        IsPrime o = new IsPrime();
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入数字：");
        long l = sc.nextLong();
        long startTime = System.currentTimeMillis();
        Boolean r = o.isPrime_old(l);
        long endTime = System.currentTimeMillis();
        System.out.println(r);
        System.out.println("共用时间：" + (endTime - startTime) + "ms");
        main(args);
    }

    public boolean isPrime(long n) {
        boolean[] b = new boolean[]{true};
        long t = (long) Math.sqrt(n) + 1;
        long l = t / 2;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch1 = new CountDownLatch(2);
        ExecutorService es = Executors.newFixedThreadPool(2);
        es.execute(() -> {
            for (long i = 2L; i <= l; i++) {
                if (n % i == 0) {
                    b[0] = false;
                    latch.countDown();
                }
            }
            latch1.countDown();
        });
        es.execute(() -> {
            long l1 = Math.max(l, 2L);
            for (long i = l1; i < t; i++) {
                if (n % i == 0) {
                    b[0] = false;
                    latch.countDown();
                }
            }
            latch1.countDown();
        });
        new Thread(() -> {
            try {
                latch1.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }).start();
        try {
            latch.await();
            es.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return b[0];
    }

    public boolean isPrime_old(long n) {
        boolean r = true;
        for (long i = 2L; i <= Math.sqrt(n); i++) {
            if (n % i == 0) r = false;
        }
        return r;
    }
}