package pers.zhc.tools.pi;


import pers.zhc.tools.utils.ScrollEditText;

class PiJNI {
    static {
        System.loadLibrary("pi");
    }

    StringBuilder sb;
    ScrollEditText o = null;

    /**
     * 生成Pi
     *
     * @param bN 小数点后位数
     */
    native void gen(int bN);

    @SuppressWarnings("unused")
    public void O(int a) {
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
        this.sb.append(a);
    }
}