package pers.zhc.tools.words

import pers.zhc.jni.sqlite.SQLite3

/**
 * @author bczhc
 */
class WordDatabase {
    // TODO: refactoring needed
    companion object {
        fun checkExistence(database: SQLite3, word: String): Boolean {
            return database.hasRecord("SELECT word FROM word WHERE word IS ?", arrayOf(word))
        }

        fun addWord(database: SQLite3, word: String) {
            database.execBind(
                "INSERT INTO word(word, addition_time) VALUES(?, ?)", arrayOf(word, System.currentTimeMillis())
            )
        }
    }
}