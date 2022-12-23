package pers.zhc.tools.tasknotes

import java.io.Serializable

data class Record(
    val description: String,
    val mark: TaskMark,
    val time: Time,
    val creationTime: Long,
) : Serializable