/*翟灿hhh*/
package pers.zhc.tools.jni;

import androidx.annotation.Size;
import pers.zhc.tools.pi.JNICallback;
import pers.zhc.tools.stcflash.JNIInterface;

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

        public static native float byteArrayToFloat(@Size(min = 4) byte[] bytes, int offset);

        public static native int byteArrayToInt(@Size(min = 4) byte[] bytes, int offset);
    }

    public static class MAllocTest {
        static {
            loadLib();
        }

        public static native long alloc(long size);
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
        public static native long open(String path) throws RuntimeException;

        /**
         * Close sqlite database
         *
         * @param id the associated id
         */
        public static native void close(long id) throws RuntimeException;

        /**
         * Execute a sqlite command.
         *
         * @param id  the associated id
         * @param cmd command
         */
        public static native void exec(long id, String cmd, SqliteExecCallback callback) throws RuntimeException;

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
        public static native long compileStatement(long id, String sql) throws RuntimeException;

        public static class Statement {
            static {
                loadLib();
            }

            /* Statement methods start: */
            public static native void bind(long stmtId, int row, int a) throws RuntimeException;

            public static native void bind(long stmtId, int row, long a) throws RuntimeException;

            public static native void bind(long stmtId, int row, double a) throws RuntimeException;

            public static native void bindText(long stmtId, int row, String s) throws RuntimeException;

            public static native void bindNull(long stmtId, int row) throws RuntimeException;

            public static native void reset(long stmtId) throws RuntimeException;

            public static native void bindBlob(long stmtId, int row, byte[] bytes, int size) throws RuntimeException;

            public static native void step(long stmtId) throws RuntimeException;

            public static native void finalize(long stmtId) throws RuntimeException;

            /**
             * Get cursor.
             *
             * @param stmtId native statement object address
             * @return native cursor object address
             */
            public static native long getCursor(long stmtId);

            /**
             * Call this rather than {@link #step(long)} when the statement returns values like `select` statements.
             *
             * @param stmtId statement native address
             * @return {@value pers.zhc.tools.utils.sqlite.SQLite3#SQLITE_ROW} if succeeded, otherwise others.
             */
            public static native int stepRow(long stmtId);

            /**
             * @param stmtId statement native address
             * @param name   column name
             * @return index, the leftmost value is 0
             */
            public static native int getIndexByColumnName(long stmtId, String name);
            /* Statement methods end. */
        }

        public static class Cursor {
            static {
                loadLib();
            }

            /* Cursor methods start. */
            public static native void reset(long cursorId) throws RuntimeException;

            public static native boolean step(long cursorId) throws RuntimeException;

            public static native byte[] getBlob(long cursorId, int column);

            public static native String getText(long cursorId, int column);

            public static native double getDouble(long cursorId, int column);

            public static native long getLong(long cursorId, int column);

            public static native int getInt(long cursorId, int column);
            /* Cursor methods end. */
        }

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
         *
         * @param str the text to be digested
         * @return result
         */
        public static native String myDigest(String str);
    }

    public static class Struct {
        static {
            loadLib();
        }

        public static final int MODE_BIG_ENDIAN = 0;
        public static final int MODE_LITTLE_ENDIAN = 1;

        public static native void packShort(short value, @Size(min = 2) byte[] dest, int offset, int mode);

        public static native void packInt(int value, @Size(min = 4) byte[] dest, int offset, int mode);

        public static native void packLong(long value, @Size(min = 8) byte[] dest, int offset, int mode);

        public static native void packFloat(float value, @Size(min = 4) byte[] dest, int offset, int mode);

        public static native void packDouble(double value, @Size(min = 8) byte[] dest, int offset, int mode);

        public static native short unpackShort(@Size(min = 2) byte[] bytes, int offset, int mode);

        public static native int unpackInt(@Size(min = 4) byte[] bytes, int offset, int mode);

        public static native long unpackLong(@Size(min = 8) byte[] bytes, int offset, int mode);

        public static native float unpackFloat(@Size(min = 4) byte[] bytes, int offset, int mode);

        public static native double unpackDouble(@Size(min = 8) byte[] bytes, int offset, int mode);
    }

    public static class SysInfo {
        static {
            loadLib();
        }

        /**
         * Get the seconds since boot
         * @return seconds
         */
        public static native long getUptime();

        /**
         * Get the number of current processes
         * @return count
         */
        public static native short getProcessesCount();
    }

    public static class Magic {
        static {
            loadLib();
        }

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
}
