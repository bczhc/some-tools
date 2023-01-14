package pers.zhc.tools.bus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.json.JSONArray
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.BusLinesListViewItemViewBinding
import pers.zhc.tools.databinding.BusQueryActivityBinding
import pers.zhc.tools.utils.AdapterWithClickListener
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.readToString
import java.io.IOException
import java.net.URL

/**
 * @author bczhc
 */
class BusQueryMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = BusQueryActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val linesRV = bindings.linesRv
        val queryBtn = bindings.queryBtn
        val lineNumET = bindings.lineNumEt.editText

        queryBtn.setOnClickListener {
            val input = lineNumET.text.toString()
            val lineNum: Int
            try {
                lineNum = Integer.parseInt(input)
            } catch (_: NumberFormatException) {
                return@setOnClickListener
            }

            fetchAndSetListView(lineNum, linesRV)
        }
    }

    class BusLineInfo(
        val busLineName: String,
        val startStationName: String,
        val endStationName: String,
        val runPathId: String,
    )

    inner class BusLineItemAdapter(val context: Context, val resource: Int, val objects: MutableList<BusLineInfo>) :
        AdapterWithClickListener<BusLineItemAdapter.MyViewHolder>() {
        inner class MyViewHolder(view: View) : ViewHolder(view) {
            private val bindings = BusLinesListViewItemViewBinding.bind(view)
            val startStationNameTV = bindings.startStationNameTv
            val endStationNameTV = bindings.endStationNameTv
            val lineNumTV = bindings.lineNumTv
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val inflate = View.inflate(context, R.layout.bus_lines_list_view_item_view, null)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val busInfo = objects[position]
            holder.startStationNameTV.text = busInfo.startStationName
            holder.endStationNameTV.text = busInfo.endStationName
            holder.lineNumTV.text = busInfo.busLineName
        }

        override fun getItemCount(): Int {
            return objects.size
        }
    }

    private fun fetchAndSetListView(lineNum: Int, linesRV: RecyclerView) {
        Thread {
            val resultJSON =
                syncFetchResultJSON("http://61.177.44.242:8080/BusSysWebService/bus/allStationOfRPName?name=$lineNum")
            if (resultJSON == null) {
                ToastUtils.show(this, R.string.bus_no_data)
                return@Thread
            }

            val lines = resultJSON["lines"] as JSONArray

            val busLineInfoList = ArrayList<BusLineInfo>()
            for (i in 0 until lines.length()) {
                val lineObject = lines[i] as JSONObject
                val busLineName = lineObject["runPathName"] as String
                val startStationName = lineObject["startName"] as String
                val endStationName = lineObject["endName"] as String
                val runPathId = lineObject["runPathId"] as String
                busLineInfoList.add(BusLineInfo(busLineName, startStationName, endStationName, runPathId))
            }

            runOnUiThread {
                val myArrayAdapter =
                    BusLineItemAdapter(this, android.R.layout.simple_list_item_1, busLineInfoList).apply {
                        this.setOnItemClickListener { position, _ ->
                            val intent = Intent(this@BusQueryMainActivity, BusLineDetailActivity::class.java)
                            intent.putExtra(
                                BusLineDetailActivity.EXTRA_RUN_PATH_ID,
                                busLineInfoList[position].runPathId
                            )
                            startActivity(intent)
                        }
                    }
                linesRV.adapter = myArrayAdapter
                linesRV.layoutManager = LinearLayoutManager(this)
                linesRV.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            }
        }.start()
    }

    companion object {
        fun syncFetchResultJSON(url: String): JSONObject? {
            try {
                val inputStream = URL(url).openStream()
                val read = inputStream.readToString()
                inputStream.close()
                val jsonObject = JSONObject(read)
                if (jsonObject["status"] as String != "SUCCESS") {
                    return null
                }
                return jsonObject["result"] as JSONObject
            } catch (_: IOException) {
            }
            return null
        }
    }
}