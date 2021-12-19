/*翟灿hhh*/
package pers.zhc.tools.jni;

import androidx.annotation.Size;
import pers.zhc.tools.fourierseries.InputPoint;
import pers.zhc.tools.pi.JNICallback;
import pers.zhc.tools.stcflash.JNIInterface;

/**
 * @author bczhc
 */
public class JNI {
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
            void callback(int a);
        }
    }

    public static class FloatingBoard {

        public static native float byteArrayToFloat(@Size(min = 4) byte[] bytes, int offset);

        public static native int byteArrayToInt(@Size(min = 4) byte[] bytes, int offset);


        public static native void packStrokeInfo3_1(
                @Size(min = 12) byte[] dest,
                int color,
                float width,
                float blurRadius
        );
    }

    public static class MAllocTest {

        public static native long alloc(long size);
    }

    public static class CharactersCounter {

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

    public static class JniDemo {
        public static native String hello(Object context, String name);
    }

    public static class StcFlash {

        public interface EchoCallback {
            void print(String s);

            void flush();
        }

        public static native void burn(String portPath, String hexFilePath, JNIInterface jniInterface, EchoCallback echoCallback);
    }

    public static class Diary {

        /**
         * Use SHA256 blah blah.
         *
         * @param str the text to be digested
         * @return result
         */
        public static native String myDigest(String str);
    }

    public static class SysInfo {

        /**
         * Get the seconds since boot
         *
         * @return seconds
         */
        public static native long getUptime();

        /**
         * Get the number of current processes
         *
         * @return count
         */
        public static native short getProcessesCount();
    }

    public static class Magic {

        public static final int MAGIC_NONE = 0x0000000; /* No flags */
        public static final int MAGIC_DEBUG = 0x0000001; /* Turn on debugging */
        public static final int MAGIC_SYMLINK = 0x0000002; /* Follow symlinks */
        public static final int MAGIC_COMPRESS = 0x0000004; /* Check inside compressed files */
        public static final int MAGIC_DEVICES = 0x0000008; /* Look at the contents of devices */
        public static final int MAGIC_MIME_TYPE = 0x0000010; /* Return the MIME type */
        public static final int MAGIC_CONTINUE = 0x0000020; /* Return all matches */
        public static final int MAGIC_CHECK = 0x0000040; /* Print warnings to stderr */
        public static final int MAGIC_PRESERVE_ATIME = 0x0000080; /* Restore access time on exit */
        public static final int MAGIC_RAW = 0x0000100; /* Don't convert unprintable chars */
        public static final int MAGIC_ERROR = 0x0000200; /* Handle ENOENT etc as real errors */
        public static final int MAGIC_MIME_ENCODING = 0x0000400; /* Return the MIME encoding */
        public static final int MAGIC_MIME = (MAGIC_MIME_TYPE | MAGIC_MIME_ENCODING);
        public static final int MAGIC_APPLE = 0x0000800; /* Return the Apple creator/type */
        public static final int MAGIC_EXTENSION = 0x1000000; /* Return a /-separated list of extensions */
        public static final int MAGIC_COMPRESS_TRANSP = 0x2000000; /* Check inside compressed files but not report compression */
        public static final int MAGIC_NODESC = (MAGIC_EXTENSION | MAGIC_MIME | MAGIC_APPLE);
        public static final int MAGIC_NO_CHECK_COMPRESS = 0x0001000; /* Don't check for compressed files */
        public static final int MAGIC_NO_CHECK_TAR = 0x0002000; /* Don't check for tar files */
        public static final int MAGIC_NO_CHECK_SOFT = 0x0004000; /* Don't check magic entries */
        public static final int MAGIC_NO_CHECK_APPTYPE = 0x0008000; /* Don't check application type */
        public static final int MAGIC_NO_CHECK_ELF = 0x0010000; /* Don't check for elf details */
        public static final int MAGIC_NO_CHECK_TEXT = 0x0020000; /* Don't check for text files */
        public static final int MAGIC_NO_CHECK_CDF = 0x0040000; /* Don't check for cdf files */
        public static final int MAGIC_NO_CHECK_CSV = 0x0080000; /* Don't check for CSV files */
        public static final int MAGIC_NO_CHECK_TOKENS = 0x0100000; /* Don't check tokens */
        public static final int MAGIC_NO_CHECK_ENCODING = 0x0200000; /* Don't check text encodings */
        public static final int MAGIC_NO_CHECK_JSON = 0x0400000; /* Don't check for JSON files */

        /**
         * Initialize libmagic
         *
         * @param flag the default flag
         * @return the native object pointer address
         */
        public static native long init(int flag);

        /**
         * Finalize libmagic
         *
         * @param addr native pointer
         */
        public static native void close(long addr);

        /**
         * Load the magic.mgc database file
         *
         * @param addr native pointer
         * @param path filepath
         * @throws RuntimeException on failure
         */
        public static native void load(long addr, String path) throws RuntimeException;

        /**
         * Set the flags that are used to describe a file<br>
         * See the constant above
         *
         * @param addr native pointer
         * @param flag flag
         */
        public static native void setFlag(long addr, int flag);

        /**
         * Get the description of a file
         *
         * @param addr native pointer
         * @param path filepath
         * @return description
         */
        public static native String file(long addr, String path);
    }

    public static class Transfer {
        public static native void send(byte[] ipv4, short port, String msg);

        public static native void startAsyncReceive(short port, Callback callback);

        public static native byte[] getLocalIpv4();

        public static native String getLocalIpInfo();

        public interface Callback {
            /**
             * @param msg   received message or error message
             * @param error if the result has errors
             */
            void onResult(String msg, boolean error);
        }
    }

    public static class Email {
        public static native void send(String smtpServer,
                                       String username,
                                       String password,
                                       String from,
                                       String[] to,
                                       String[] cc,
                                       String subject,
                                       String body);
    }

    public static class Utf8 {
        public static native long getCodepointIterator(String s);

        public static native boolean hasNext(long addr);

        public static native int next(long addr);

        public static native void release(long addr);

        public static native int codepointLength(String s);

        public static native String codepoint2str(int codepoint);
    }

    public static class FourierSeries {
        public interface Callback {
            void onResult(double re, double im, int n, double p);
        }

        public static final int LINEAR_PATH_EVALUATOR = 0;
        public static final int TIME_PATH_EVALUATOR = 1;

        public static native void compute(
                InputPoint[] points,
                int integralFragment,
                double period,
                int epicycleNum,
                int threadNum,
                int pathEvaluatorType,
                Callback callback
        );
    }
}
