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
    }
}