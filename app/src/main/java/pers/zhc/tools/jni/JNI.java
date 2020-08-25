/*翟灿hhh*/package pers.zhc.tools.jni;

import androidx.annotation.Size;
import pers.zhc.tools.pi.JNICallback;

/**
 * @author bczhc
 */
public class JNI {
    private static boolean hasLoadLib = false;

    private synchronized static void loadLib() {
        if (!hasLoadLib) {
            System.loadLibrary("All");
            hasLoadLib = true;
        }
    }

    public static class Codecs {

        static {
            loadLib();
        }

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
        static {
            loadLib();
        }

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
        static {
            loadLib();
        }

        public static native void floatToByteArray(@Size(min = 4) byte[] dest, float f, int offset);

        public static native void intToByteArray(@Size(min = 4) byte[] dest, int i, int offset);

        public static native float byteArrayToFloat(@Size(min = 4) byte[] bytes, int offset);

        public static native int byteArrayToInt(@Size(min = 4) byte[] bytes, int offset);
    }

    public static class MAllocTest {
        static {
            loadLib();
        }

        public static native long alloc(long size);
    }

    public static class FourierSeriesCalc {
        static {
            loadLib();
        }

        public static native void calc(double period, int epicyclesCount, Callback callback, int threadNum);

        public interface Callback {
            /**
             * callback
             *
             * @param n  n
             * @param re complex value re part
             * @param im complex value im part
             */
            void callback(double n, double re, double im);
        }
    }

    public static class Sqlite3 {
        static {
            loadLib();
            System.loadLibrary("sqlite3");
        }

        public interface SqliteExecCallback {
            /**
             * Callback when {@link Sqlite3#exec(int, String, SqliteExecCallback)} is called.
             * @param contents content in database
             * @return whether to continue search:
             * 0: interrupt searching
             * 1: continue
             */
            int callback(String[] contents);
        }

        /**
         * Create a handler hold an id linked the native database object.
         *
         * @return the id, and it's the "handler"
         */
        public static native int createHandler();

        /**
         * Release the native database associated with the id.
         *
         * @param id the id
         */
        public static native void releaseHandler(int id);

        /**
         * Open sqlite database.
         *
         * @param id   the associated id
         * @param path sqlite database path, if not exists, it'll create a new sqlite database
         */
        public static native void open(int id, String path);

        /**
         * Close sqlite database
         *
         * @param id the associated id
         */
        public static native void close(int id);

        /**
         * Execute a sqlite command.
         *
         * @param id  the associated id
         * @param cmd command
         */
        public static native void exec(int id, String cmd, SqliteExecCallback callback);
    }

    public static class CharactersCounter {
        static {
            loadLib();
        }

        /**
         * Create a handler used to find the native handler that holds the result map
         * @return id
         */
        public static native int createHandler();

        /**
         * Release handler
         * @param id id
         */
        public static native void releaseHandler(int id);

        /**
         * Count duplicated characters in text
         * @param s string
         */
        public static native void count(int id, String s);

        /**
         * clear native result data
         * @param id id
         */
        public static native void clearResult(int id);

        /**
         * get result json
         * @param id id
         * @return json string
         */
        public static native String getResultJson(int id);
    }
}
