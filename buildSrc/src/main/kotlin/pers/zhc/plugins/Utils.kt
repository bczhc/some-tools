package pers.zhc.plugins

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun copy(src: File, dest: File) {
    val buf = ByteArray(4096)
    var readLen: Int
    val inputStream = FileInputStream(src)
    val outputStream = FileOutputStream(dest, false)
    while (true) {
        readLen = inputStream.read(buf)
        if (readLen <= 0) break
        outputStream.write(buf, 0, readLen)
        outputStream.flush()
    }
    outputStream.close()
    inputStream.close()
}