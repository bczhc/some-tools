/**
 * @author bczhc
 */
package pers.zhc.tools.utils

import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun File.readToString(charset: Charset = StandardCharsets.UTF_8): String {
    val bytes = ByteArray(this.length().toInt())
    val fis = FileInputStream(this)
    val read = fis.read(bytes)
    return String(bytes, 0, read, charset)
}

fun InputStream.readToString(charset: Charset = StandardCharsets.UTF_8): String {
    val readBytes = this.readBytes()
    return String(readBytes, charset)
}

fun InputStream.writeTo(out: OutputStream) = this.copyTo(out, Long.MAX_VALUE)

/**
 * Returns false if the limit is exceeded and the copy is interrupted
 */
fun InputStream.copyTo(out: OutputStream, limit: Long): Boolean {
    val buf = ByteArray(4096)
    var readLen: Int
    var bytesCopied = 0L

    while (true) {
        readLen = this.read(buf)
        if (readLen == -1) break
        out.write(buf, 0, readLen)
        out.flush()
        bytesCopied += readLen
        if (bytesCopied > limit) {
            return false
        }
    }
    return true
}

class MkdirException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

class DeleteException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

class IORuntimeException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

fun File.requireDelete() {
    if (this.exists()) {
        if (!this.delete()) {
            throw DeleteException()
        }
    }
}

fun File.requireMkdir() {
    if (!this.exists()) {
        if (!this.mkdir()) {
            throw MkdirException()
        }
    }
}

fun File.requireMkdirs() {
    if (!this.exists()) {
        if (!this.mkdirs()) {
            throw MkdirException()
        }
    }
}

fun InputStream.readAll(): ByteArray {
    val output = ByteArrayOutputStream()
    this.copyTo(output)
    return output.toByteArray()
}

