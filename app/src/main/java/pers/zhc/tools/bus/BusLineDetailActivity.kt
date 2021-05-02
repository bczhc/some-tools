package pers.zhc.tools.bus

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.bus_line_detail_activity.*
import org.json.JSONArray
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class BusLineDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_line_detail_activity)

        val intent = intent
        val runPathId = intent.getStringExtra(INTENT_RUN_PATH_ID)!!

        val startStationNameTV = start_station_name_tv!!
        val endStationNameTV = end_station_name_tv!!
        val switchBusDirectionBtn = switch_bus_direction!!
        val busRunTimeTV = bus_run_time_tv!!
        val busIntervalTV = bus_interval_tv!!
        val busTotalCountTV = bus_total_count_tv!!
        val busLineDetailLL = bus_line_detail_ll!!

        switchBusDirectionBtn.setOnClickListener {

        }

        val lock = Any()

        Thread {
            val busInfo = syncFetchBusInfo(runPathId)!!
            runOnUiThread {
                synchronized(lock) {
                    startStationNameTV.text = getString(R.string.bus_start_station_tv, busInfo.startStationName)
                    endStationNameTV.text = getString(R.string.bus_end_station_tv, busInfo.endStationName)
                    busRunTimeTV.text =
                        getString(R.string.bus_line_run_time_from_to_tv, busInfo.busStartTime, busInfo.busEndTime)
                    busIntervalTV.text = getString(R.string.bus_line_run_interval_minute, busInfo.busInterval)
                    title = busInfo.busLineName
                }
            }
        }.start()

        Thread {
            val result = syncFetchBusRunInfo(runPathId)
            if (result == null) {
                ToastUtils.show(this, R.string.bus_no_data)
                return@Thread
            }

            runOnUiThread {
                synchronized(lock) {
                    busTotalCountTV.text = getString(R.string.bus_line_bus_total_count_tv, result.size)
                }
            }
        }.start()

        Thread {

        }.start()
    }

    private fun syncFetchBusInfo(runPathId: String): BusInfo? {
        val result =
            BusQueryMainActivity.syncFetchResultJSON("http://61.177.44.242:8080/BusSysWebService/common/busQuery?flag=1&runPathId=$runPathId")
                ?: return null

        val startTime: String = when {
            result.has("startTime1") -> {
                result["startTime1"] as String
            }
            result.has("startTime") -> {
                result["startTime"] as String
            }
            else -> {
                return null
            }
        }

        val endTime: String = when {
            result.has("endTime1") -> {
                result["startTime1"] as String
            }
            result.has("endTime") -> {
                result["endTime"] as String
            }
            else -> {
                return null
            }
        }

        return BusInfo(result["runPathName"] as String,
            result["startStation"] as String,
            result["endStation"] as String,
            startTime,
            endTime,
            result["busInterval"] as String)
    }

    private fun syncFetchBusRunInfo(runPathId: String): List<ABusRun>? {
        val result =
            BusQueryMainActivity.syncFetchResultJSON("http://61.177.44.242:8080/BusSysWebService/bus/gpsForRPF?flag=1&rpId=$runPathId")
                ?: return null
        val list = result["lists"] as JSONArray

        val ret = ArrayList<ABusRun>()

        for (i in 0 until list.length()) {
            val busRunJSONObject = list[i] as JSONObject
            val busStationName = busRunJSONObject["busStationName"] as String
            val outState = busRunJSONObject["outState"] as String

            val stopping = outState == "0"
            ret.add(ABusRun(busStationName, stopping))
        }

        return ret
    }

    private fun syncFetchBusStationsInfo(runPathId: String, direction: Direction): List<Station>? {
        val result =
            BusQueryMainActivity.syncFetchResultJSON("http://61.177.44.242:8080/BusSysWebService/bus/searchSSR?rpId=$runPathId")
        ?: return null


        return null
    }

    enum class Direction {
        DIRECTION_1,
        DIRECTION_2
    }

    class BusInfo(
        val busLineName: String,
        val startStationName: String,
        val endStationName: String,
        val busStartTime: String,
        val busEndTime: String,
        val busInterval: String,
    )

    class ABusRun(val busStationName: String, stopping: Boolean)

    class Station(val busStationName: String, val orginal: Int)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bus_line_detail_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                // refresh bus line detail info

            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * intent string extra
         */
        const val INTENT_RUN_PATH_ID = "runPathId"
    }
}