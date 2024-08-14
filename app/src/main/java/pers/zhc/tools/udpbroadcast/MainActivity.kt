package pers.zhc.tools.udpbroadcast

import android.annotation.SuppressLint
import android.os.Bundle
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.UdpBroadcastMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.coroutineLaunchIo
import pers.zhc.tools.utils.withMain

class MainActivity : BaseActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = UdpBroadcastMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.bindBtn.setOnClickListener {
            val port = bindings.portEt.text.toString().toInt()
            bindings.bindBtn.isEnabled = false
            bindings.et.setText("")

            coroutineLaunchIo {
                val result = runCatching {
                    val socket =
                        aSocket(SelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress("255.255.255.255", port)) {
                            reuseAddress = true
                        }
                    val datagram = socket.receive()
                    val bytes = datagram.packet.readBytes()
                    JNI.Utf8.fromBytesLossy(bytes)
                }
                withMain {
                    bindings.bindBtn.isEnabled = true
                    result.onSuccess {
                        bindings.et.setText(it)
                    }.onFailure {
                        bindings.et.setText("Error:\n$it")
                    }
                }
            }
        }
    }
}
