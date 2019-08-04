package com.zhc.tools.pi;


import android.widget.EditText;

class PiJNI {
    static int[] ints;

    PiJNI(int bN) {
        ints = new int[bN / 4 + 1];
    }

    PiJNI() {
    }

    StringBuilder sb;
    EditText o = null;

    static {
        System.loadLibrary("pi");
    }


    /**
     * 生成Pi
     *
     * @param bN 小数点后位数
     */
    native void gen(int bN);

    @SuppressWarnings("unused")
    public void O(int a, int i) {
//        CountDownLatch latch = new CountDownLatch(1);
        /*runOnUiThread(() -> {
            o.setText(String.format("%s%s", s, o.getText().toString()));
            latch.countDown();
        });*/

        /*try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
//        this.sb.append(s);
        new T(i).f(a);
    }

    static class T {
        private int i;

        T(int index) {
            this.i = index;
        }

        void f(int a) {
            ints[this.i] = a;
        }
    }
}