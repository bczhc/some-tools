package pers.zhc.tools.characterscounter

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class CharactersCounter {
    private val id: Int = JNI.CharactersCounter.createHandler()

    fun count(s: String) {
        JNI.CharactersCounter.count(id, s)
    }

    fun getResultJson(): String {
        return JNI.CharactersCounter.getResultJson(id)
    }

    fun clearResult() {
        JNI.CharactersCounter.clearResult(id)
    }
}