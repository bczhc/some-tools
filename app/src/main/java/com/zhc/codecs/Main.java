package com.zhc.codecs;

import android.app.Activity;
import android.widget.TextView;
import com.zhc.JNI;

class Main {
    /**
     * JNIMethodCall
     *
     * @param f        f1
     * @param dF       f2
     * @param dT       doType
     *                 0: qmcDecode
     *                 1: kwmDecode
     *                 21: Base128 encode
     *                 22: Base128 decode
     * @param tv       progressTextView
     * @param activity activityContext
     * @param mode     delete srcFile: 1
     * @return status -2
     */
    @SuppressWarnings("Duplicates")
    int JNI_Decode(String f, String dF, int dT, TextView tv, Activity activity, int mode) {
        switch (dT) {
            case 0:
                return new JNI(tv, activity).qmcDecode(f, dF, mode);
            case 1:
                return new JNI(tv, activity).kwmDecode(f, dF, mode);
            case 21:
                return new JNI(tv, activity).Base128_encode(f, dF);
            case 22:
                return new JNI(tv, activity).Base128_decode(f, dF);
        }
        return -2;
    }
}