package pers.zhc.tools.stcflash

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.android.synthetic.main.file_picker_rl_activity.*
import kotlinx.android.synthetic.main.stc_flash_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils

class FlashMainActivity : BaseActivity() {
    companion object {
        private const val ACTION_USB_PERMISSION = "pers.zhc.tools.USB_PERMISSION"
    }

    private lateinit var serialPool: SerialPool
    private lateinit var connectSwitch: SwitchCompat
    private lateinit var usbManager: UsbManager
    private var port: UsbSerialPort? = null
    private var device: UsbDevice? = null
    private lateinit var permissionIntent: PendingIntent
    private var burning = false
    private lateinit var switchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var hexFilePathET: EditText

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        device.apply {
                            ToastUtils.show(context, R.string.connected)

                            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                            if (availableDrivers.isEmpty()) {
                                ToastUtils.show(this@FlashMainActivity, getString(R.string.empty_driver))
                                setSwitchChecked(false)
                                return@apply
                            }
                            val usbSerialDriver = availableDrivers[0]
                            val connection = usbManager.openDevice(usbSerialDriver.device)
                            if (connection == null) {
                                ToastUtils.show(this@FlashMainActivity, getString(R.string.null_connection))
                                setSwitchChecked(false)
                                return@apply
                            }

                            port = usbSerialDriver.ports[0]
                            if (port == null) {
                                ToastUtils.show(this@FlashMainActivity, getString(R.string.null_port))
                                setSwitchChecked(false)
                                return@apply
                            }
                            port!!.open(connection)
                            port!!.setParameters(
                                1200,
                                UsbSerialPort.DATABITS_8,
                                UsbSerialPort.STOPBITS_1,
                                UsbSerialPort.PARITY_NONE
                            )
                            serialPool = SerialPool(port!!)
                        }
                    } else {
                        setSwitchChecked(false)
                        ToastUtils.show(context, R.string.permission_denied)
                    }
                }
            }
        }
    }

    private fun registerUsbReceiver() {
        permissionIntent =
            PendingIntent.getBroadcast(this, RequestCode.REQUEST_USB_PERMISSION, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    private fun requestUsbPermission() {
        val values = usbManager.deviceList.values
        if (values.isNotEmpty()) {
            val firstDevice = values.first()
            usbManager.requestPermission(firstDevice, permissionIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerUsbReceiver()
        setContentView(R.layout.stc_flash_activity)
        connectSwitch = connect_switch!!
        hexFilePathET = hex_file_path_et!!
        val burnBtn = burn_btn!!
        val callbackET = callback_et!!
        val pickFileBtn = pick_file_btn!!

        pickFileBtn.setOnClickListener {
            val intent = Intent(this, FilePicker::class.java)
            intent.putExtra("option", FilePicker.PICK_FILE)
            startActivityFromChild(this, intent, RequestCode.START_ACTIVITY_0)
        }

        val echoCallback = object : JNI.StcFlash.EchoCallback {
            override fun print(s: String?) {
                runOnUiThread {
                    s!!
                    callbackET.text = getString(R.string.concat, callbackET.text.toString(), s)
                }
                kotlin.io.print(s)
            }

            override fun flush() {
                System.out.flush()
            }
        }

        switchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
                requestUsbPermission()

                val deviceList = usbManager.deviceList
                if (deviceList.size <= 0) {
                    ToastUtils.show(this, R.string.null_device)
                    setSwitchChecked(false)
                    return@OnCheckedChangeListener
                }
            } else {
                try {
                    port?.close()
                } catch (_: Exception) {
                }
                port = null
            }
        }

        connectSwitch.setOnCheckedChangeListener(switchListener)

        burnBtn.setOnClickListener {
            callbackET.text = getString(R.string.nul)
            Thread {
                if (port == null) {
                    runOnUiThread {
                        ToastUtils.show(this, getString(R.string.please_connect_first))
                    }
                    return@Thread
                }
                val hexFilePath = hexFilePathET.text.toString()
                val jniInterface = JNIInterface(port, serialPool)
                try {
                    burning = true
                    JNI.StcFlash.burn(device!!.deviceName, hexFilePath, jniInterface, echoCallback)
                    burning = false
                } catch (e: Exception) {
                    Common.showException(e, this)
                }
            }.start()
        }
    }

    fun setSwitchChecked(checked: Boolean) {
        this.connectSwitch.setOnCheckedChangeListener(null)
        this.connectSwitch.isChecked = checked
        this.connectSwitch.setOnCheckedChangeListener(this.switchListener)
    }

    override fun finish() {
        if (burning) {
            ToastUtils.show(this, R.string.please_wait_until_burning_finished)
            return
        }
        if (port != null && port!!.isOpen) {
            serialPool.stop()
        }
        super.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            val picked = data.getStringExtra("result") ?: return
            hexFilePathET.setText(picked)
        }
    }
}