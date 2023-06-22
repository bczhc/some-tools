package pers.zhc.tools.crashhandler

import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pers.zhc.tools.Info
import pers.zhc.tools.MyApplication
import pers.zhc.tools.MyApplication.Companion.HTTP_CLIENT_DEFAULT
import pers.zhc.tools.jni.JNI.BZip3
import pers.zhc.tools.utils.ByteSize
import pers.zhc.tools.utils.fromJsonOrNull
import pers.zhc.tools.utils.nullMap

object CrashReportUploader {
    data class Response(val code: Int, val message: String)

    suspend fun request(data: ByteArray): Response? {
        return withContext(Dispatchers.IO) {
            val httpResponse = HTTP_CLIENT_DEFAULT
                .post(Info.serverRootURL + "/app/some-tools/crash-report") {
                    setBody(ByteArrayContent(data))
                }

            var response: Response? = null
            if (httpResponse.status == HttpStatusCode.OK) {
                response =
                    MyApplication.GSON.fromJsonOrNull(httpResponse.bodyAsText(), JsonObject::class.java).nullMap {
                        Response(
                            (it.get("status") ?: return@nullMap null).asInt,
                            (it.get("message") ?: return@nullMap null).asString
                        )
                    }
            }
            response
        }
    }

    fun upload(
        activity: CrashReportActivity,
        content: String,
        updateUiSuccess: () -> Unit,
        updateUiFailure: (message: String?) -> Unit
    ) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val compressed = BZip3.compress(bytes, 1 * ByteSize.MIB)
        activity.lifecycleScope.launch {
            val result = runCatching {
                request(compressed)
            }
            if (result.isFailure) {
                updateUiFailure(result.exceptionOrNull()!!.toString())
                return@launch
            }
            val response = result.getOrNull()
            withContext(Dispatchers.Main) {
                if (response != null && response.code == 0) {
                    updateUiSuccess()
                } else {
                    updateUiFailure(response?.message)
                }
            }
        }
    }
}
