package pers.zhc.tools.utils

class Stopwatch {
    private val startTime = System.currentTimeMillis()

    fun stop(): Long {
        return System.currentTimeMillis() - startTime
    }

    companion object {
        fun start() = Stopwatch()
    }
}