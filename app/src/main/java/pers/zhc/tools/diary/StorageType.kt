package pers.zhc.tools.diary

import androidx.annotation.StringRes
import pers.zhc.tools.R
import java.util.NoSuchElementException

enum class StorageType(val enumInt: Int, @StringRes val textResInt: Int) {
    RAW(0, R.string.raw),
    TEXT(1, R.string.text),
    IMAGE(2, R.string.image),
    AUDIO(3, R.string.audio);

    companion object {
        fun from(enumInt: Int): StorageType {
            val values = values()
            values.forEach {
                if (it.enumInt == enumInt) return it
            }
            throw NoSuchElementException()
        }
    }
}