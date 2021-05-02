package pers.zhc.tools.bus

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
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
    private lateinit var runPathId: String
    private lateinit var busLineDetailLL: BusLineDetailLL
    private lateinit var busTotalCountTV: TextView
    private lateinit var busIntervalTV: TextView
    private lateinit var startStationNameTV: TextView
    private lateinit var busRunTimeTV: TextView
    private lateinit var endStationNameTV: TextView
    private var currentDirection = Direction.DIRECTION_1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_line_detail_activity)

        val intent = intent
        runPathId = intent.getStringExtra(INTENT_RUN_PATH_ID)!!

        startStationNameTV = start_station_name_tv!!
        endStationNameTV = end_station_name_tv!!
        busRunTimeTV = bus_run_time_tv!!
        busIntervalTV = bus_interval_tv!!
        busTotalCountTV = bus_total_count_tv!!
        busLineDetailLL = bus_line_detail_ll!!
        val switchBusDirectionBtn = switch_bus_direction!!

        switchBusDirectionBtn.setOnClickListener {
            // reverse the value
            currentDirection = if (currentDirection == Direction.DIRECTION_1) {
                Direction.DIRECTION_2
            } else Direction.DIRECTION_1

            busLineDetailLL.removeAllStations()
            asyncSetPageOnUiThread()
        }

        asyncSetPageOnUiThread()
    }

    private fun asyncSetPageOnUiThread() {
        val lock = Any()

        Thread {
            val busInfo = syncFetchBusInfo(runPathId)!!
            runOnUiThread {
                synchronized(lock) {
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
            val busStationList = syncFetchBusStationsInfo(runPathId, currentDirection)
            if (busStationList == null) {
                ToastUtils.show(this, R.string.bus_no_data)
                return@Thread
            }

            runOnUiThread {
                synchronized(lock) {
                    startStationNameTV.text =
                        getString(R.string.bus_start_station_tv, busStationList[0].busStationName)
                    endStationNameTV.text = getString(R.string.bus_end_station_tv,
                        busStationList[busStationList.size - 1].busStationName)

                    busStationList.forEach {
                        busLineDetailLL.addStation(it)
                    }
                }
            }
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
            val outState = busRunJSONObject["outstate"] as String

            val stopping = outState == "0"
            ret.add(ABusRun(busStationName, stopping))
        }

        return ret
    }

    private fun syncFetchBusStationsInfo(runPathId: String, direction: Direction): List<Station>? {
        val result =
            BusQueryMainActivity.syncFetchResultJSON("http://61.177.44.242:8080/BusSysWebService/bus/searchSSR?rpId=$runPathId")
                ?: return null

        val ret = ArrayList<Station>()
        val jsonArray = when (direction) {
            Direction.DIRECTION_1 -> {
                result["shangxing"] as JSONArray
            }
            Direction.DIRECTION_2 -> {
                result["xiaxing"] as JSONArray
            }
        }

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray[i] as JSONObject
            val busStationName = jsonObject["busStationName"] as String
            ret.add(Station(busStationName))
        }

        return ret
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

    class Station(val busStationName: String)

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