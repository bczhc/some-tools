package pers.zhc.tools.bus

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import pers.zhc.tools.R
import pers.zhc.tools.databinding.BusLineDetailStationViewBinding
import pers.zhc.tools.utils.Common

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

    fun addStation(station: BusLineDetailActivity.Station): StationLL {
        val stationView = getStationView(station)
        this.addView(stationView)
        return stationView
    }

    fun removeAllStations() {
        this.removeAllViews()
    }

    private fun getStationView(station: BusLineDetailActivity.Station): StationLL {
        val inflate =
            View.inflate(context, R.layout.bus_line_detail_station_view, null)
        val bindings = BusLineDetailStationViewBinding.bind(inflate)
        bindings.ordinalTv.text =
            context.getString(R.string.bus_line_detail_station_ordinal_tv, this.childCount + 1 /* ordinal */)
        val stationNameTV = bindings.stationNameTv

        stationNameTV.text = station.busStationName.join('\n')
            .replace('（', '︵')
            .replace('）', '︶')

            .replace('(', '︵')
            .replace(')', '︶')

        val llRoot = bindings.llRoot
        llRoot.station = station
        llRoot.busState = TopNodeView.BusState.ARRIVED
        llRoot.getTopNodeView().setBusMarkDotCount(0)
        return llRoot
    }

    /**
     * [busRunList]: fetched using [BusLineDetailActivity.syncFetchBusRunInfo]
     */
    fun setupBusesDisplay(busRunList: List<BusLineDetailActivity.ABusRun>) {
        busRunList.forEach {
            if (it.arrived) {
                val child = findChildByStationId(it.busStationId)!!
                val topNodeView = child.getTopNodeView()
                topNodeView.setBusMarkDotCount(topNodeView.getBusMarkDotCount() + 1)
                topNodeView.setBusState(TopNodeView.BusState.ARRIVED)
            } else {
                val onBusStationLlIndex = getChildIndexByStationId(it.busStationId)
                Common.doAssertion(onBusStationLlIndex != -1)
                val nextChild = getChildAt(onBusStationLlIndex + 1) as StationLL
                if (nextChild.busState == TopNodeView.BusState.ARRIVED) {
                    // no such "on road" bus `StationLL` node, insert a new one
                    val newStationLL = View.inflate(context, R.layout.bus_line_detail_station_view, null)
                        .findViewById<StationLL>(R.id.ll_root)
                    val topNodeView = newStationLL.getTopNodeView()
                    topNodeView.setBusState(TopNodeView.BusState.ON_ROAD)
                    topNodeView.setBusMarkDotCount(1)
                    newStationLL.setStationInfoVisibility(View.GONE)
                    addView(newStationLL, onBusStationLlIndex + 1)
                } else {
                    // is the "on road" bus `StationLL` node, change some parameters of `TopNodeView`
                    // hide the below station info `LinearLayout`, with 0 width and height
                    val topNodeView = nextChild.getTopNodeView()
                    topNodeView.setBusState(TopNodeView.BusState.ON_ROAD)
                    topNodeView.setBusMarkDotCount(topNodeView.getBusMarkDotCount() + 1)
                }
            }
        }
    }

    private fun findChildByStationId(stationId: String): StationLL? {
        val index = getChildIndexByStationId(stationId)
        return if (index != -1) {
            getChildAt(index) as StationLL
        } else {
            null
        }
    }

    private fun getChildIndexByStationId(stationId: String): Int {
        for (i in 0 until this.childCount) {
            val child = getChildAt(i) as StationLL
            if (child.station != null && child.station!!.busStationId == stationId) {
                return i
            }
        }
        return -1
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
    var busState: TopNodeView.BusState? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun getTopNodeView(): TopNodeView {
        return this.findViewById(R.id.top_line_node_view)
    }

    /**
     * [visibility] is a value among [View.INVISIBLE], [View.VISIBLE], [View.GONE]
     */
    fun setStationInfoVisibility(visibility: Int) {
        this.findViewById<LinearLayout>(R.id.below_bus_info_ll).visibility = visibility
    }
}