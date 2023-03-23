package pers.zhc.tools.utils

import io.ktor.client.statement.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

suspend fun HttpResponse.bytesFlow(): Flow<ByteArray> {
    val body = this.bodyAsChannel()
    return flow {
        while (!body.isClosedForRead) {
            val packet = body.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                emit(packet.readBytes())
            }
        }
    }
}
