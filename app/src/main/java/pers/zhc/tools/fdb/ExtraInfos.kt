package pers.zhc.tools.fdb

import android.graphics.Matrix
import com.google.gson.Gson
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.fromJsonOrNull
import pers.zhc.tools.utils.getValuesNew
import pers.zhc.tools.utils.withCompiledStatement
import pers.zhc.tools.views.HSVAColorPickerRL.SavedColor

/**
 * @author bczhc
 */
class ExtraInfos(
    val isLockingStroke: Boolean?,
    val lockedDrawingStrokeWidth: Float?,
    val lockedEraserStrokeWidth: Float?,
    val savedColors: List<SavedColor>?,
    val defaultTransformation: FloatArray?,
    val layersInfo: List<LayerInfo>?
) {
    companion object {
        private fun queryExtraInfos(db: SQLite3): String? {
            var jsonString: String? = null
            db.withCompiledStatement("SELECT extra_infos FROM info") {
                val cursor = it.cursor
                jsonString = if (cursor.step()) {
                    cursor.getText(0)
                } else null
            }
            return jsonString
        }

        /**
         * returns null if [db] has no such database field or the info string
         * it stores has invalid json syntax
         */
        fun getExtraInfos(db: SQLite3): ExtraInfos? {
            return Gson().fromJsonOrNull(
                queryExtraInfos(db) ?: return null,
                ExtraInfos::class.java
            ) ?: (Gson().fromJsonOrNull(
                queryExtraInfos(db) ?: return null,
                OldExtraInfos::class.java
            ) ?: return null).toNewExtraInfos()
        }
    }

    @Suppress("SpellCheckingInspection")
    private class OldMatrixData(
        val MSCALE_X: Float,
        val MSKEW_X: Float,
        val MTRANS_X: Float,
        val MSKEW_Y: Float,
        val MSCALE_Y: Float,
        val MTRANS_Y: Float,
        val MPERSP_0: Float,
        val MPERSP_1: Float,
        val MPERSP_2: Float
    ) {
        fun toMatrix(): Matrix {
            return Matrix().apply {
                val data = floatArrayOf(
                    MSCALE_X,
                    MSKEW_X,
                    MTRANS_X,
                    MSKEW_Y,
                    MSCALE_Y,
                    MTRANS_Y,
                    MPERSP_0,
                    MPERSP_1,
                    MPERSP_2
                )
                this.setValues(data)
            }
        }
    }

    private class OldExtraInfos(
        val isLockingStroke: Boolean?,
        val lockedDrawingStrokeWidth: Float?,
        val lockedEraserStrokeWidth: Float?,
        val savedColors: List<SavedColor>?,
        val defaultTransformation: OldMatrixData?,
        val layersInfo: List<LayerInfo>?
    ) {
        fun toNewExtraInfos(): ExtraInfos? {
            return ExtraInfos(
                isLockingStroke,
                lockedDrawingStrokeWidth,
                lockedEraserStrokeWidth,
                savedColors,
                (defaultTransformation ?: return null).toMatrix().getValuesNew(),
                layersInfo
            )
        }
    }
}
