package pers.zhc.tools.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * @author bczhc
 * returns null on json exception occurred
 */
fun <T> Gson.fromJsonOrNull(json: String, classOfT: Class<T>): T? {
    return try {
        this.fromJson(json, classOfT)
    } catch (e: JsonSyntaxException) {
        null
    }
}

inline fun <reified T> Gson.fromJsonOrNull(json: String): T? {
    return this.fromJsonOrNull(json, T::class.java)
}
