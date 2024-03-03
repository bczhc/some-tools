package pers.zhc.tools.texteditor

data class ExifEntry(
    val tag: String,
    val tagDesc: String,
    val valueDisplay: String,
    val valueReadable: String,
    val valueInternal: String,
)
