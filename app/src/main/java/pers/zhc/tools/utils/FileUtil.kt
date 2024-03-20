package pers.zhc.tools.utils

import pers.zhc.tools.MyApplication
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.random.Random
import kotlin.random.nextUInt

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

fun tmpFile(): File {
    val cacheDir = MyApplication.appContext.cacheDir!!
    while (true) {
        val f = File(cacheDir, Random.nextUInt().toString())
        if (!f.exists()) {
            return f
        }
    }
}

fun File.checkAddExtension(ext: String): File {
    return if (this.extension != ext) File(this.path + ".$ext") else this
}

fun File.copyFrom(stream: InputStream) {
    stream.writeTo(this.outputStream())
}
