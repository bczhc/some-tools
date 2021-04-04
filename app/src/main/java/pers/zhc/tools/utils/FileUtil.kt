package pers.zhc.tools.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @author bczhc
 */
class FileUtil {
    companion object {
        @JvmStatic
        fun copy(src: String, dst: String) {
            val buf = ByteArray(4096)
            var readLen: Int
            val srcIS = FileInputStream(src)
            val dstOS = FileOutputStream(dst)

            while (true) {
                readLen = srcIS.read(buf)
                if (readLen <= 0) break
                dstOS.write(buf, 0, readLen)
                dstOS.flush()
            }

            srcIS.close()
            dstOS.close()
        }

        @JvmStatic
        fun copy(src: File, dst: File) {
            copy(src.path, dst.path)
        }
    }
}