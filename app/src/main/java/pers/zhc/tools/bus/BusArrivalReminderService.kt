package pers.zhc.tools.bus

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * @author bczhc
 */
class BusArrivalReminderService: Service() {
    private lateinit var direction: BusLineDetailActivity.Direction
    private lateinit var stationId: String
    private lateinit var busRunId: String
    private val TAG = BusArrivalReminderService::class.java.name

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "$TAG started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent!!
        busRunId = intent.getStringExtra(EXTRA_RUN_PATH_ID)!!
        stationId = intent.getStringExtra(EXTRA_BUS_STATION_ID)!!
        direction = intent.getSerializableExtra(EXTRA_DIRECTION)!! as BusLineDetailActivity.Direction
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "$TAG destroyed")
    }

    companion object {
        /**
         * string extra
         */
        const val EXTRA_RUN_PATH_ID = "busRunId"
        /**
         * string extra
         * the destination station id
         */
        const val EXTRA_BUS_STATION_ID = "busStationId"

        /**
         * serializable extra
         * the bus direction
         */
        const val EXTRA_DIRECTION = "direction"
    }
}