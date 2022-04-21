package pers.zhc.tools.transfer

enum class Mark(val enumInt: Int) {
    FILE(1), TEXT(2), TAR(3);

    companion object {
        fun fromEnumInt(enumInt: Int): Mark? {
            return values().firstOrNull() { it.enumInt == enumInt }
        }
    }
}