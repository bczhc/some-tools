package pers.zhc.tools.codecs;

import android.app.Activity;
import android.widget.TextView;

import java.util.List;

class JNIMain {
    /**
     * JNIMethodCall
     *
     * @param activity    activityContext
     * @param f           f1
     * @param dF          f2
     * @param dT          doType
     *                    0: qmcDecode
     *                    1: kwmDecode
     *                    21: Base128 encode
     *                    22: Base128 decode
     * @param tv          progressTextView
     * @param savedConfig savedConfig
     * @return status 0
     */
    int JNI_Decode(String f, String dF, int dT, TextView tv, Activity activity, List<List<String>> savedConfig) {
        switch (dT) {
            case 0:
                return new JNI(tv, activity).qmcDecode(f, dF, getMode(savedConfig, 0));
            case 1:
                return new JNI(tv, activity).kwmDecode(f, dF, getMode(savedConfig, 1));
            case 21:
                return new JNI(tv, activity).Base128_encode(f, dF, getMode(savedConfig, 2));
            case 22:
                return new JNI(tv, activity).Base128_decode(f, dF, getMode(savedConfig, 3));
        }
        return 0;
    }

    /**
     * mode:
     * delete source file: 1
     * not ..:0
     *
     * @param savedConfig config lists
     * @param i           index
     * @return mode integer
     */
    private int getMode(List<List<String>> savedConfig, @SuppressWarnings("SameParameterValue") int i) {
        boolean b = Boolean.parseBoolean(savedConfig.get(i).get(2));
        return b ? 1 : 0;
    }
}