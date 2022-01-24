package pers.zhc.tools.utils

import pers.zhc.util.Assertion
import pers.zhc.util.IOUtils
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * @author bczhc
 */
class ZipUtils {
    companion object {
        fun decompressSingleFile(src: File, dest: File, progress: ProgressCallback?) {
            val fileSize = src.length()

            val zipFile = ZipFile(src)
            val entries = zipFile.entries()
            Assertion.doAssertion(entries.hasMoreElements())
            val inputStream = zipFile.getInputStream(entries.nextElement())

            val outputStream = dest.outputStream()

            var readLen: Int
            val buf = ByteArray(4096)
            var count = 0L
            while (true) {
                readLen = inputStream.read(buf)
                if (readLen == -1) break
                outputStream.write(buf, 0, readLen)
                count += readLen

                progress?.invoke((count.toDouble() / fileSize.toDouble()).toFloat())
            }

            outputStream.flush()
            outputStream.close()

            inputStream.close()

            progress?.invoke(1F)
        }
    }
}

typealias ProgressCallback = (Float) -> Unit