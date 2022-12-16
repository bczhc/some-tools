package pers.zhc.tools.tasknotes

data class Record(
    val description: String,
    val mark: TaskMark,
    val time: Time,
    val creationTime: Long,
)