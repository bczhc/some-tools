package com.zhc.codec;

import android.widget.TextView;
import com.zhc.JNI;

class Main {
    @SuppressWarnings("Duplicates")
    int JNI_Decode(String f, String dF, int dT, TextView tv) {
        switch (dT) {
            case 0:
                return new JNI(tv).qmcDecode(f, dF);
            case 1:
                return new JNI(tv).kwmDecode(f, dF);
        }
        return -2;
    }
}