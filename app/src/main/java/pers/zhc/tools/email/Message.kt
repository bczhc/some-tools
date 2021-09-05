package pers.zhc.tools.email

/**
 * @author bczhc
 */
class Message(val to: String, val subject: String) {
    var body: String? = null
    var cc: String? = null
}