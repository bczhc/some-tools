package pers.zhc.tools.tasknotes

import pers.zhc.tools.R

enum class TaskMark(val enumInt: Int) {
    START(0), END(1);

    fun getStringRes(): Int {
        return when (this) {
            START -> R.string.task_notes_task_mark_start
            END -> R.string.task_notes_task_mark_end
        }
    }

    companion object {
        fun from(enumInt: Int): TaskMark? {
            return when (enumInt) {
                START.enumInt -> START
                END.enumInt -> END
                else -> null
            }
        }
    }
}
