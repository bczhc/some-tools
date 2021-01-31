package pers.zhc.tools.stcflash

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.android.synthetic.main.stc_flash_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils

class FlashMainActivity : BaseActivity() {
    private var port: UsbSerialPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stc_flash_activity)
        val connectSwitch = connect_switch
        val hexFilePathET = hex_file_path_et
        val burnBtn = burn_btn

        connectSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                if (availableDrivers.isEmpty()) {
                    ToastUtils.show(this, getString(R.string.empty_driver))
                    return@setOnCheckedChangeListener
                }

                val usbSerialDriver = availableDrivers[0]
                val connection = usbManager.openDevice(usbSerialDriver.device)
                if (connection == null) {
                    ToastUtils.show(this, getString(R.string.null_connection))
                    return@setOnCheckedChangeListener
                }

                port = usbSerialDriver.ports[0]
                if (port == null) {
                    ToastUtils.show(this, getString(R.string.null_port))
                    return@setOnCheckedChangeListener
                }
                port!!.open(connection)
                port!!.setParameters(1200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            } else {
                port?.close()
            }
        }

        burnBtn.setOnClickListener {
            if (port == null) {
                ToastUtils.show(this, getString(R.string.please_connect_first))
                return@setOnClickListener
            }
            val hexFilePath = hexFilePathET.text.toString()
            val jniInterface = JNIInterface(port)
            try {
                JNI.StcFlash.burn(hexFilePath, jniInterface)
            } catch (e: Exception) {
                Common.showException(e, this)
            }
        }
    }

    override fun finish() {
        if (port != null && port!!.isOpen) {
            port!!.close()
        }
        super.finish()
    }
}