package pers.zhc.tools.transfer

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TransferReceiveActivityBinding
import pers.zhc.tools.filebrowser.TextFileBrowser
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.jni.JNI.ByteSize
import pers.zhc.tools.utils.*
import java.io.File
import java.util.*
import kotlin.math.min

/**
 * @author bczhc
 */
class TransferReceiveActivity : BaseActivity() {
    val results = ArrayList<ReceivingResult>()
    private lateinit var listenPortET: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TransferReceiveActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        listenPortET = bindings.listenPort.editText
        val receiveStartButton = bindings.receiveStartBtn

        val receiveDir = File(Common.getAppMainExternalStoragePathFile(this), "transfer")
        receiveDir.requireMkdir()

        val recyclerView = bindings.recyclerView
        recyclerView.addDividerLines()
        recyclerView.setLinearLayoutManager()

        val listAdapter = ListAdapter(this, results)

        recyclerView.adapter = listAdapter

        listAdapter.setOnItemClickListener { position, _ ->

            val openPath = { path: String ->
                // open this path in file manager
                val uri = path.toUri()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "*/*")
                startActivity(intent)
            }

            val result = results[position]
            if (result is ReceivingResult.Companion.Success) {
                val path = result.path
                when (result.mark) {
                    Mark.FILE -> openPath(File(path).parent!!)
                    Mark.TEXT -> {
                        startActivity(Intent(this, TextFileBrowser::class.java).apply {
                            putExtra(TextFileBrowser.EXTRA_PATH, path)
                        })
                    }
                    Mark.TAR -> openPath(path)
                }
            }
        }

        receiveStartButton.setOnClickListener {
            val listenPort = listenPortET.text.toString()
            if (listenPort.isEmpty()) {
                ToastUtils.show(this, getString(R.string.transfer_empty_port_toast))
                return@setOnClickListener
            }
            val port = listenPort.toInt()

            val callback = object : JNI.Transfer.Callback {
                override fun onReceiveResult(mark: Int, receivingTime: Long, size: Long, path: String) {
                    synchronized(this) {
                        results.add(
                            ReceivingResult.Companion.Success(
                                Mark.fromEnumInt(mark)!!, receivingTime, size, path
                            )
                        )
                        runOnUiThread {
                            listAdapter.notifyItemInserted(results.size - 1)
                        }
                    }
                }

                override fun onError(msg: String) {
                    synchronized(this) {
                        results.add(
                            ReceivingResult.Companion.Failure(
                                msg, System.currentTimeMillis()
                            )
                        )
                        runOnUiThread {
                            listAdapter.notifyItemInserted(results.size - 1)
                        }
                    }
                }
            }

            try {
                val listenerAddress = JNI.Transfer.asyncStartServer(port, receiveDir.path, callback)
                ToastUtils.show(this, listenerAddress.toString())
            } catch (e: Exception) {
                Common.showException(e, this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.transfer_receive, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.qr_code -> {
                showQrCode()
            }
            else -> {
                return false
            }
        }
        return true
    }

    private fun showQrCode() {
        val wifiIp = WifiUtils.getWifiIpString(this)
        val socketAddrString = "$wifiIp:${listenPortET.text}"

        val screenSize = DisplayUtil.getScreenSize(this)
        val min = min(screenSize.x, screenSize.y)
        val qrSide = (min.toDouble() * .7).toInt()

        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(socketAddrString, BarcodeFormat.QR_CODE, qrSide, qrSide)

        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
        }
        Dialog(this).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setContentView(imageView)
        }.show()
    }

    class ListAdapter(val context: Context, val results: ReceivingResults) :
        AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        class MyViewHolder(val itemView: ReceiveItemView) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            return MyViewHolder(ReceiveItemView(context))
        }

        override fun getItemCount(): Int {
            return results.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val result = results[position]
            val itemView = holder.itemView as ReceiveItemView
            when (result) {
                is ReceivingResult.Companion.Success -> {
                    val timeStr = Date(result.receivingTime).toString()
                    itemView.setSuccess(
                        timeStr,
                        ByteSize.toHumanReadable(result.size, true),
                        context.getString(result.mark.getTypeStringRes())
                    )
                }
                is ReceivingResult.Companion.Failure -> {
                    val timeStr = Date(result.time).toString()
                    itemView.setFailure(
                        timeStr, result.errMsg
                    )
                }
            }
        }
    }


    abstract class ReceivingResult {
        companion object {
            class Success(val mark: Mark, val receivingTime: Long, val size: Long, val path: String) : ReceivingResult()

            class Failure(val errMsg: String, val time: Long) : ReceivingResult()
        }
    }

}

private fun Mark.getTypeStringRes(): Int {
    return when (this) {
        Mark.FILE -> R.string.transfer_receive_type_file
        Mark.TEXT -> R.string.transfer_receive_type_text
        Mark.TAR -> R.string.transfer_receive_type_files
    }
}

typealias ReceivingResults = ArrayList<TransferReceiveActivity.ReceivingResult>
