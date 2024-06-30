package pers.zhc.tools.barcode

import java.io.Serializable

data class ScanResult(
    val content: String,
    val rawBytes: ByteArray?,
    val errorCorrectionLevel: String?,
    val formatName: String?,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScanResult

        if (content != other.content) return false
        if (!rawBytes.contentEquals(other.rawBytes)) return false
        if (errorCorrectionLevel != other.errorCorrectionLevel) return false
        if (formatName != other.formatName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        result = 31 * result + errorCorrectionLevel.hashCode()
        result = 31 * result + formatName.hashCode()
        return result
    }
}
