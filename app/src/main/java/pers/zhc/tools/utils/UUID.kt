package pers.zhc.tools.utils

object UUID {
    fun random() = java.util.UUID.randomUUID().toString()

    fun randomNoDash() = random().replace("-", "")
}