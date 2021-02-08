/*翟灿hhh*/
package pers.zhc.tools.jni;

import androidx.annotation.Size;
import pers.zhc.tools.pi.JNICallback;
import pers.zhc.tools.stcflash.JNIInterface;
import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;

/**
 * @author bczhc
 */
public class JNI {
    private static boolean hasLoadedLib = false;

    private synchronized static void loadLib() {
        if (!hasLoadedLib) {
            System.loadLibrary("Main");
            hasLoadedLib = true;
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

    public static class FourierSeries {
        static {
            loadLib();
        }

        public static native void calc(ArrayList<ComplexValue> points, double period, int epicyclesCount, Callback callback, int threadNum, int integralN);

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
        }

        /**
         * Open sqlite database.
         *
         * @param path sqlite database path, if not exists, it'll create a new sqlite database
         * @return the associated id which is the address of an handler object in JNI.
         */
        public static native long open(String path);

        /**
         * Close sqlite database
         *
         * @param id the associated id
         */
        public static native void close(long id);

        /**
         * Execute a sqlite command.
         *
         * @param id  the associated id
         * @param cmd command
         */
        public static native void exec(long id, String cmd, SqliteExecCallback callback);

        /**
         * Check if the database is corrupted.
         *
         * @param id id
         * @return result
         */
        public static native boolean checkIfCorrupt(long id);

        /**
         * Compile sqlite statement.
         *
         * @param id  id
         * @param sql sqlite statement
         * @return the address of the statement object in JNI, which is a "statement object handler"
         */
        public static native long compileStatement(long id, String sql) throws Exception;

        /* Statement methods start: */
        public static native void bind(long statId, int row, int a) throws Exception;

        public static native void bind(long statId, int row, long a) throws Exception;

        public static native void bind(long statId, int row, double a) throws Exception;

        public static native void bindText(long statId, int row, String s) throws Exception;

        public static native void bindNull(long statId, int row) throws Exception;

        public static native void reset(long statId) throws Exception;

        public static native void bindBlob(long statId, int row, byte[] bytes, int size) throws Exception;

        public static native void step(long statId) throws Exception;

        public static native void finalize(long statId) throws Exception;
        /* Statement methods end. */


        public interface SqliteExecCallback {
            /**
             * Callback when {@link Sqlite3#exec(long, String, SqliteExecCallback)} is called.
             *
             * @param contents content in database
             * @return whether to continue searching:
             * 0: continue
             * non-zero: interrupt searching
             */
            int callback(String[] contents);
        }
    }

    public static class CharactersCounter {
        static {
            loadLib();
        }

        /**
         * Create a handler used to find the native handler that holds the result map
         *
         * @return id
         */
        public static native int createHandler();

        /**
         * Release handler
         *
         * @param id id
         */
        public static native void releaseHandler(int id);

        /**
         * Count duplicated characters in text
         *
         * @param s string
         */
        public static native void count(int id, String s);

        /**
         * clear native result data
         *
         * @param id id
         */
        public static native void clearResult(int id);

        /**
         * get result json
         *
         * @param id id
         * @return json string
         */
        public static native String getResultJson(int id);
    }

    public static class JniTest {
        static {
            loadLib();
        }

        public static native void call(int fd);
    }

    public static class StcFlash {
        static {
            loadLib();
        }

        public interface EchoCallback {
            void print(String s);

            void flush();
        }

        public static native void burn(String portPath, String hexFilePath, JNIInterface jniInterface, EchoCallback echoCallback);
    }

    public static class Diary {
        static {
            loadLib();
        }

        /**
         * Use SHA256 blah blah.
         * @param str the text to be digested
         * @return result
         */
        public static native String myDigest(String str);
    }
}
