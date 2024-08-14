package pers.zhc.tools.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

fun LifecycleOwner.coroutineLaunch(
    context: CoroutineContext = Dispatchers.Default,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch(context, block = block)
}

fun LifecycleOwner.coroutineLaunchIo(block: suspend CoroutineScope.() -> Unit) {
    coroutineLaunch(Dispatchers.IO, block)
}

suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main, block)
}

suspend fun <T> withIo(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, block)
}
