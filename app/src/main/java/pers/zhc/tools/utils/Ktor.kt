package pers.zhc.tools.utils

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import pers.zhc.tools.MyApplication.Companion.GSON
import kotlin.reflect.full.memberProperties

object FormDataUtils {
    inline fun <reified T : Any> fromObject(obj: T): FormDataContent {
        return FormDataContent(Parameters.build {
            // only applicable for fields with @JvmField
            T::class.memberProperties.filter {
               true
            }.forEach {
                val name = it.name
                val value = it.getter.call(obj)
                value?.let { v ->
                    append(name, v.toString())
                }
            }
        })
    }
}

inline fun <reified T : Any> HttpRequestBuilder.setFormDataBody(src: T) {
    this.setBody(FormDataUtils.fromObject(src))
}

suspend inline fun <reified T : Any> HttpResponse.bodyAsJson(): T? {
    return GSON.fromJsonOrNull<T>(this.bodyAsText())
}
