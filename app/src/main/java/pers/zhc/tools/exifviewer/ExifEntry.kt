package pers.zhc.tools.exifviewer

data class ExifEntry(
    val tagId: UShort,
    val tagDisplay: String,
    val tagDesc: String,
    val valueDisplay: String,
    val valueReadable: String,
    val valueInternal: String,
)
