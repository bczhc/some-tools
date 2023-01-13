package pers.zhc.tools.utils

class ThreadWrapper<T>(private val runnable: () -> T) {
    @Volatile
    private var result: T? = null
    val javaThread = Thread { result = runnable() }

    fun join(): T {
        javaThread.join()
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}

fun <T> thread(
    isDaemon: Boolean = false,
    contextClassLoader: ClassLoader? = null,
    name: String? = null,
    priority: Int = -1,
    block: () -> T
): ThreadWrapper<T> {
    return ThreadWrapper(block).also { t ->
        val javaThread = t.javaThread
        if (isDaemon) javaThread.isDaemon = true
        contextClassLoader?.let { javaThread.contextClassLoader = it }
        name?.let { javaThread.name = it }
        if (priority > 0) javaThread.priority = priority
        javaThread.start()
    }
}
