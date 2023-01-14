package pers.zhc.tools.transfer

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TransferMainActivityBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.WifiUtils

/**
 * @author bczhc
 */
class TransferMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TransferMainActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val sendButton = bindings.sendBtn
        val receiveButton = bindings.receiveBtn
        val ipInfoTV = bindings.ipInfoTv
        val wifiIpTv = bindings.ipTv

        ipInfoTV.text = JNI.Ip.getLocalIpInfo()!!

        sendButton.setOnClickListener {
            startActivity(Intent(this, TransferSendActivity::class.java))
        }
        receiveButton.setOnClickListener {
            startActivity(Intent(this, TransferReceiveActivity::class.java))
        }
        wifiIpTv.text = getString(R.string.transfer_wifi_ip_tv, WifiUtils.getWifiIpString(this))
    }
}