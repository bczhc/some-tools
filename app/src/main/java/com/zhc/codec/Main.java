package com.zhc.codec;

import android.widget.TextView;
import com.zhc.JNI;

class Main {
    /**
     * JNIMethodCall
     *
     * @param f  f1
     * @param dF f2
     * @param dT doType
     *           0: qmcDecode
     *           1: kwmDecode
     *           21: Base128 encode
     *           22: Base128 decode
     * @param tv progressTextView
     * @return -2
     */
    @SuppressWarnings("Duplicates")
    int JNI_Decode(String f, String dF, int dT, TextView tv) {
        switch (dT) {
            case 0:
                return new JNI(tv).qmcDecode(f, dF);
            case 1:
                return new JNI(tv).kwmDecode(f, dF);
            case 21:
                return new JNI(tv).Base128_encode(f, dF);
            case 22:
                return new JNI(tv).Base128_decode(f, dF);
        }
        return -2;
    }
}