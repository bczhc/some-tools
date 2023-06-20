package pers.zhc.tools.utils

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream

fun ByteArray.decompressBzip2(): ByteArray {
    val inputStream = ByteArrayInputStream(this)
    val decompressor = BZip2CompressorInputStream(inputStream)
    return decompressor.readAll()
}
