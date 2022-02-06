package pers.zhc.tools.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * @author bczhc
 */
fun <T> Gson.fromJsonOrNull(json: String, classOfT: Class<T>): T? {
    return try {
        this.fromJson(json, classOfT)
    } catch (e: JsonSyntaxException) {
        null
    }
}