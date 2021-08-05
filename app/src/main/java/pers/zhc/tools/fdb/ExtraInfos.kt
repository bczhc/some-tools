package pers.zhc.tools.fdb

import android.graphics.Matrix
import pers.zhc.tools.views.HSVAColorPickerRL.SavedColor

/**
 * @author bczhc
 */
data class ExtraInfos(
    val isLockingStroke: Boolean,
    val lockedDrawingStrokeWidth: Float,
    val lockedEraserStrokeWidth: Float,
    val savedColors: ArrayList<SavedColor>,
    val defaultTransformation: Matrix
)
