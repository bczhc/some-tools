package pers.zhc.tools.pi;


import android.widget.EditText;

class PiJNI {
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