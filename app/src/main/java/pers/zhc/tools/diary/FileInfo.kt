package pers.zhc.tools.diary

import java.io.Serializable

class FileInfo(
    val content: String,
    val additionTimestamp: Long,
    val storageTypeEnumInt: Int,
    val description: String,
    val identifier: String,
) : Serializable