package pers.zhc.tools.email

/**
 * @author bczhc
 */
class Message(val to: Array<String>, val subject: String) {
    var body: String? = null
    var cc: Array<String>? = null
}