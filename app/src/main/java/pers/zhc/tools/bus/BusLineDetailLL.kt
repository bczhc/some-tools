package pers.zhc.tools.bus

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.bus_line_detail_station_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil

/**
 * @author bczhc
 */
class BusLineDetailLL : LinearLayout {
    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        this.orientation = HORIZONTAL
    }

    fun addStation(station: BusLineDetailActivity.Station) {
        val stationView = getStationView(station)
        this.addView(stationView)
    }

    fun removeAllStations() {
        this.removeAllViews()
    }

    private fun getStationView(station: BusLineDetailActivity.Station): View {
        val inflate =
            View.inflate(context, R.layout.bus_line_detail_station_view, null).ll_root!!
        inflate.ordinal_tv!!.text =
            context.getString(R.string.bus_line_detail_station_ordinal_tv, this.childCount + 1 /* ordinal */)
        val stationNameTV = inflate.station_name_tv!!

        stationNameTV.text = station.busStationName.join('\n')
            .replace('（', '︵').replace('）', '︶')

        inflate.setOnClickListener {
            val dialog = DialogUtil.createConfirmationAlertDialog(context, { _, _ ->
                setBusArrivalReminder(station.busStationId)
            }, context.getString(R.string.bus_ask_for_setting_bus_arrival_reminder_dialog_title, station.busStationName))
            dialog.show()
        }

        inflate.station = station
        return inflate
    }

    private fun setBusArrivalReminder(busStationId: String) {
        val intent = Intent(context, BusArrivalReminderService::class.java)
        context.startService(intent)
    }

    /**
     * [busRunList]: fetched using [BusLineDetailActivity.syncFetchBusRunInfo]
     */
    fun setupBusesDisplay(busRunList: List<BusLineDetailActivity.ABusRun>) {
        busRunList.forEach {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child !is StationLL) continue
                val station = child.station!!

                if (it.busStationId == station.busStationId) {
                    if (it.stopping) {
                        child.setTopNodeViewBusMarkState(TopLineNodeView.BusState.ARRIVED)
                    } else {
                        val topLineNodeView = TopLineNodeView(context)
                        topLineNodeView.setBusState(TopLineNodeView.BusState.ON_ROAD)
                        this.addView(topLineNodeView, i + 1);
                    }
                }
            }
        }
    }
}

private fun String.join(c: Char): String {
    val sb = StringBuilder()
    val charArray = this.toCharArray()
    for (ch in charArray) {
        sb.append(ch).append(c)
    }
    sb.deleteCharAt(sb.length - 1)
    return sb.toString()
}

class StationLL : LinearLayout {
    var station: BusLineDetailActivity.Station? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setTopNodeViewBusMarkState(state: TopLineNodeView.BusState) {
        this.top_line_node_view.setBusState(state)
    }
}