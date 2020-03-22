package pers.zhc.tools.codecs;

import pers.zhc.tools.jni.JNI;

import java.util.List;

class jniCall {
    /**
     * JNIMethodCall
     *
     * @param f           f1
     * @param dF          f2
     * @param dT          doType
     *                    0: qmcDecode
     *                    1: kwmDecode
     *                    21: Base128 encode
     *                    22: Base128 decode
     * @param savedConfig savedConfig
     * @param callback    progress callback
     * @return status 0
     */
    static int jniDecode(String f, String dF, int dT, List<List<String>> savedConfig, JNI.Codecs.Callback callback) {
        switch (dT) {
            case 0:
                pers.zhc.tools.jni.JNI.Codecs.qmcDecode(f, dF, getMode(savedConfig, 0), callback);
                break;
            case 1:
                pers.zhc.tools.jni.JNI.Codecs.kwmDecode(f, dF, getMode(savedConfig, 1), callback);
                break;
            case 21:
                pers.zhc.tools.jni.JNI.Codecs.Base128_encode(f, dF, getMode(savedConfig, 2), callback);
                break;
            case 22:
                pers.zhc.tools.jni.JNI.Codecs.Base128_decode(f, dF, getMode(savedConfig, 3), callback);
                break;
            default:
                break;
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
    private static int getMode(List<List<String>> savedConfig, @SuppressWarnings("SameParameterValue") int i) {
        boolean b = Boolean.parseBoolean(savedConfig.get(i).get(2));
        return b ? 1 : 0;
    }
}