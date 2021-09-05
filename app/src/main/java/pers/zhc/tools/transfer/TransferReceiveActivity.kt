package pers.zhc.tools.transfer

import android.os.Bundle
import kotlinx.android.synthetic.main.transfer_receive_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class TransferReceiveActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_receive_activity)

        val listenPortET = listen_port!!.editText
        val receiveStartBtn = receive_start_btn!!
        val outputET = output_et!!.editText

        receiveStartBtn.setOnClickListener {
            val port = listenPortET.text.toString().toShortOrNull() ?: run {
                ToastUtils.show(this@TransferReceiveActivity, R.string.transfer_invalid_port_toast)
                return@setOnClickListener
            }
            Receiver.startAsyncReceive(port) { msg, error ->
                if (!error) {
                    runOnUiThread {
                        outputET.setText(msg)
                    }
                } else {
                    ToastUtils.showException(this, Exception(msg))
                }
            }
        }
    }
}
