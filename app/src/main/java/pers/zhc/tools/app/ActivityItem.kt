package pers.zhc.tools.app

import android.app.Activity
import androidx.annotation.StringRes

/**
 * @author bczhc
 */
class ActivityItem(
    @JvmField
    @StringRes
    val textRes: Int,
    @JvmField
    val activityClass: Class<out Activity>
)