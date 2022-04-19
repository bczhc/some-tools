package pers.zhc.tools.transfer

import android.os.Bundle
import kotlinx.android.synthetic.main.transfer_receive_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.requireMkdir
import java.io.File

/**
 * @author bczhc
 */
class TransferReceiveActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_receive_activity)

        val listenPortET = listen_port!!.editText
        val receiveStartButton = receive_start_btn!!

        val savingPath = File(Common.getExternalStoragePath(this), "transfer")
        savingPath.requireMkdir()

        receiveStartButton.setOnClickListener {
            val listenPort = listenPortET.text.toString()
            if (listenPort.isEmpty()) {
                ToastUtils.show(this, getString(R.string.transfer_empty_port_toast))
                return@setOnClickListener
            }
            val port = listenPort.toInt()
            try {
                val listenerAddress =
                    JNI.Transfer.asyncStartServer(port, savingPath.path) { mark, receivingTimestamp, size, path ->
                        ToastUtils.show(this, "Received: $mark $receivingTimestamp $size $path")
                    }
//                ToastUtils.show(this, R.string.tran
//
//
//
//                sfer_server_started_toast)
                ToastUtils.show(this, listenerAddress.toString())
            } catch (e: Exception) {
                Common.showException(e, this)
            }
        }
    }
}
