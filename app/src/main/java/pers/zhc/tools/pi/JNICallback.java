package pers.zhc.tools.pi;

import pers.zhc.tools.jni.JNI;

public class JNICallback implements JNI.Pi.Callback {
    private final StringBuilder sb;

    public JNICallback(StringBuilder sb) {
        this.sb = sb;
    }

    @Override
    public void callback(int a) {
        this.sb.append(a);
    }

    StringBuilder getSb() {
        return sb;
    }
}
