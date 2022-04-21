package pers.zhc.tools.transfer

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.transfer_receive_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.jni.JNI.ByteSize
import pers.zhc.tools.utils.*
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class TransferReceiveActivity : BaseActivity() {
    val results = ArrayList<ReceivingResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_receive_activity)

        val listenPortET = listen_port!!.editText
        val receiveStartButton = receive_start_btn!!

        val receiveDir = File(Common.getAppMainExternalStoragePathFile(this), "transfer")
        receiveDir.requireMkdir()

        val recyclerView = recycler_view!!
        recyclerView.addDividerLines()
        recyclerView.setLinearLayoutManager()

        val listAdapter = ListAdapter(this, results)

        recyclerView.adapter = listAdapter

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
