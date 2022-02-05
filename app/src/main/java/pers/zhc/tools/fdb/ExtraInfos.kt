package pers.zhc.tools.fdb

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.fromJsonOrNull
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
        /**
         * returns `null` if [extraInfosJson] has bad json syntax or there's
         * no "layers" field.
         */
        fun getLayersInfo(extraInfosJson: String): List<LayerInfo>? {
            return try {
                Gson().fromJson(extraInfosJson, ExtraInfos::class.java)!!
            } catch (e: JsonSyntaxException) {
                return null
            }.layersInfo
        }

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
            )
        }
    }
}
