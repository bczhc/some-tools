package pers.zhc.tools.email

import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class Sender {
    companion object {
        fun send(account: Account, message: Message) {
            val smtpTransport = account.smtpTransport
            val credential = smtpTransport.credential
            JNI.Email.send(
                smtpTransport.server,
                credential.username,
                credential.password,
                account.headerFrom,
                message.to,
                message.cc,
                message.subject,
                message.body
            )
        }
    }
}