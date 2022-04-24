package pers.zhc.tools.utils

import android.content.Context
import android.net.wifi.WifiManager

class WifiUtils {
    companion object {
        @Suppress("DEPRECATION")
        fun getWifiIpString(context: Context): String {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager;
            val ipAddress = wm.connectionInfo.ipAddress
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        }
    }
}