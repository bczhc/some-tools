package pers.zhc.tools.bus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.bus_lines_list_view_item_view.view.*
import kotlinx.android.synthetic.main.bus_query_activity.*
import org.json.JSONArray
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
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
        setContentView(R.layout.bus_query_activity)

        val linesLV = lines_lv!!
        val queryBtn = query_btn!!
        val lineNumET = line_num_et!!.editText

        queryBtn.setOnClickListener {
            val input = lineNumET.text.toString()
            val lineNum: Int
            try {
                lineNum = Integer.parseInt(input)
            } catch (_: NumberFormatException) {
                return@setOnClickListener
            }

            fetchAndSetListView(lineNum, linesLV)
        }
    }

    class BusLineInfo(
        val busLineName: String,
        val startStationName: String,
        val endStationName: String,
        val runPathId: String,
    )

    inner class BusLineItemAdapter(context: Context, resource: Int, objects: MutableList<BusLineInfo>) :
        ArrayAdapter<BusLineInfo>(context, resource, objects) {
        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflate = View.inflate(context, R.layout.bus_lines_list_view_item_view, null)
            val startStationNameTV = inflate.start_station_name_tv
            val endStationNameTV = inflate.end_station_name_tv
            val lineNumTV = inflate.line_num_tv

            val busInfo = getItem(position)!!
            startStationNameTV.text = busInfo.startStationName
            endStationNameTV.text = busInfo.endStationName
            lineNumTV.text = busInfo.busLineName

            return inflate
        }
    }

    private fun fetchAndSetListView(lineNum: Int, linesLV: ListView) {
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
                val myArrayAdapter = BusLineItemAdapter(this, android.R.layout.simple_list_item_1, busLineInfoList)
                linesLV.adapter = myArrayAdapter

                linesLV.setOnItemClickListener { _, _, position, _ ->
                    val intent = Intent(this@BusQueryMainActivity, BusLineDetailActivity::class.java)
                    intent.putExtra(BusLineDetailActivity.EXTRA_RUN_PATH_ID, busLineInfoList[position].runPathId)
                    startActivity(intent)
                }
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