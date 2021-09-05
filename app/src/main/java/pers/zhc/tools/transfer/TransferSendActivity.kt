package pers.zhc.tools.transfer

import android.os.Bundle
import kotlinx.android.synthetic.main.transfer_send_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.util.Assertion
import java.net.InetAddress

/**
 * @author bczhc
 */
class TransferSendActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_send_activity)
        val addressET = destination_address_et!!.editText
        val messageET = msg_et!!.editText
        val sendMsgBtn = send_msg_btn!!

        sendMsgBtn.setOnClickListener {
            val address = parseAddress(addressET.text.toString()) ?: run {
                ToastUtils.show(this@TransferSendActivity, R.string.transfer_invalid_address_toast)
                return@setOnClickListener
            }
            Thread {
                InetAddress.getLocalHost()
                try {
                    JNI.Transfer.send(address.ip, address.port, messageET.text.toString())
                    ToastUtils.show(this, "Done")
                } catch (e: Exception) {
                    ToastUtils.showException(this, e)
                }
            }.start()
        }
    }

    fun parseAddress(address: String): SocketAddress? {
        if (!address.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+:[0-9]+$"))) {
            return null
        }
        val index = address.lastIndexOf(':')
        val portString = address.substring(index + 1)
        val ipString = address.substring(0, index)
        val ipNumStrings = ipString.split('.')
        Assertion.doAssertion(ipNumStrings.size == 4)

        val ip = ByteArray(4).also {
            var error = false
            it.forEachIndexed { index, _ ->
                val toInt = ipNumStrings[index].toInt()
                if (toInt < 0 || toInt > 255) {
                    error = true
                }
                it[index] = toInt.toByte()
            }
            if (error) {
                ToastUtils.show(this, R.string.transfer_invalid_address_toast)
            }
        }
        val port = portString.toShort()
        return SocketAddress(ip, port)
    }
}