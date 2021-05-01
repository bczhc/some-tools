package pers.zhc.tools.bus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import kotlinx.android.synthetic.main.bus_lines_list_view_item_view.view.*
import kotlinx.android.synthetic.main.bus_query_activity.*
import org.json.JSONArray
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.u.common.ReadIS
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch

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

    class BusLineInfo(val busLineName: String, val startStationName: String, val endStationName: String)

    private fun fetchAndSetListView(lineNum: Int, linesLV: ListView) {
        val busesInfoJSON = fetchBusesInfoJSON(lineNum)
        if (busesInfoJSON == null) {
            ToastUtils.show(this, R.string.bus_no_such_line_toast)
            return
        }
        val jsonObject = JSONObject(busesInfoJSON)
        if (jsonObject["status"] as String != "SUCCESS") {
            ToastUtils.show(this, R.string.bus_request_not_success)
            return
        }
        val lines = (jsonObject["result"] as JSONObject)["lines"] as JSONArray

        val busLineInfoList = ArrayList<BusLineInfo>()
        for (i in 0 until lines.length()) {
            val lineObject = lines[i] as JSONObject
            val busLineName = lineObject["runPathName"] as String
            val startStationName = lineObject["startName"] as String
            val endStationName = lineObject["endName"] as String
            busLineInfoList.add(BusLineInfo(busLineName, startStationName, endStationName))
        }
        class MyArrayAdapter(context: Context, resource: Int, objects: MutableList<BusLineInfo>) :
            ArrayAdapter<BusLineInfo>(context, resource, objects) {
            @SuppressLint("ViewHolder")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflate = View.inflate(context, R.layout.bus_lines_list_view_item_view, null)
                val startStationNameTV = inflate.start_station_name_tv
                val endStationNameTV = inflate.end_station_name_tv
                val lineNumTV = inflate.line_num_tv

                val function1: (View) -> Unit = {
                    val intent = Intent(this@BusQueryMainActivity, BusLineDetailActivity::class.java)
                    startActivity(intent)
                }
                val function: (v: View) -> Unit = function1
                inflate.setOnClickListener(function)

                val busInfo = getItem(position)!!
                startStationNameTV.text = busInfo.startStationName
                endStationNameTV.text = busInfo.endStationName
                lineNumTV.text = busInfo.busLineName

                return inflate
            }
        }

        val myArrayAdapter = MyArrayAdapter(this, android.R.layout.simple_list_item_1, busLineInfoList)
        linesLV.adapter = myArrayAdapter
    }

    private fun fetchBusesInfoJSON(lineNumber: Int): String? {
        val countDownLatch = CountDownLatch(1)
        var readToString: String? = null
        Thread {
            try {
                val url = URL("http://61.177.44.242:8080/BusSysWebService/bus/allStationOfRPName?name=$lineNumber")
                val inputStream = url.openStream()
                readToString = ReadIS.readToString(inputStream, StandardCharsets.UTF_8)
                inputStream.close()
            } catch (_: Exception) {
            }
            countDownLatch.countDown()
        }.start()
        countDownLatch.await()
        return readToString
    }
}