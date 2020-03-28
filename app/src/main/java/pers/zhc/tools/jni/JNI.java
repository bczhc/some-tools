package pers.zhc.tools.jni;

import android.support.annotation.Size;
import pers.zhc.tools.pi.JNICallback;

/**
 * @author bczhc
 */
public class JNI {
    static {
        System.loadLibrary("All");
    }
    public static class Codecs {

        /**
         * @param f    f
         * @param dF   dF
         * @param mode delete srcFile: 1
         * @return status
         */
        public static native int qmcDecode(String f, String dF, int mode, Callback callback);

        /**
         * @param f    f
         * @param dF   dF
         * @param mode delete srcFile: 1
         * @return status
         */
        public static native int kwmDecode(String f, String dF, int mode, Callback callback);

        public static native int Base128_encode(String f, String dF, int mode, Callback callback);

        public static native int Base128_decode(String f, String dF, int mode, Callback callback);

        public interface Callback {
            /**
             * jni callback
             *
             * @param s s
             * @param b b
             */
            @SuppressWarnings("unused")
            void callback(String s, double b);
        }
    }

    public static class Pi {

        /**
         * generate
         *
         * @param bN       小数点后位数
         * @param callback callback
         */
        public static native void gen(int bN, JNICallback callback);

        public interface Callback {
            /**
             * jni callback
             *
             * @param a pi%.4
             */
            @SuppressWarnings("unused")
            void callback(int a);
        }
    }

    public static class FloatingBoard {
        public static native void floatToByteArray(@Size(min = 4) byte[] dest, float f, int offset);

        public static native void intToByteArray(@Size(min = 4) byte[] dest, int i, int offset);

        public static native float byteArrayToFloat(@Size(min = 4) byte[] bytes, int offset);

        public static native int byteArrayToInt(@Size(min = 4) byte[] bytes, int offset);
    }

    public static class MAllocTest {
        public static native long alloc(long size);
    }
}
