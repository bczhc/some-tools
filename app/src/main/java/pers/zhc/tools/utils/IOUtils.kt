/**
 * @author bczhc
 */
package pers.zhc.tools.utils

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

fun InputStream.writeTo(out: OutputStream) {
    val buf = ByteArray(4096)
    var readLen: Int

    while (true) {
        readLen = this.read(buf)
        if (readLen == -1) break
        out.write(buf, 0, readLen)
        out.flush()
    }
}

class MkdirException: RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

class DeleteException: RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

class IORuntimeException: RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}