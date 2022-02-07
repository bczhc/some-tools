package pers.zhc.tools.fdb

import com.google.gson.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication.Companion.defaultGson
import pers.zhc.tools.utils.fromJsonOrNull
import pers.zhc.tools.utils.withCompiledStatement
import pers.zhc.tools.views.HSVAColorPickerRL.SavedColor
import java.lang.reflect.Type

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
            return Gson().newBuilder().apply {
                registerTypeAdapter(FloatArray::class.java, OldMatrixDataSerializer())
                registerTypeAdapter(LayerInfo::class.java, LayersInfoDeserializer())
                registerTypeAdapter(SavedColor::class.java, OldSavedColorDeserializer())
            }.create().fromJsonOrNull(
                queryExtraInfos(db) ?: return null,
                ExtraInfos::class.java
            )
        }
    }

    private class OldMatrixDataSerializer : JsonDeserializer<FloatArray?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): FloatArray? {
            json ?: return null

            if (json is JsonObject) {
                // check if is a "OldMatrixData" JSON object
                if (json.keySet().all { ExtraInfos::OldMatrixData.parameters.map { p -> p.name }.contains(it) }) {
                    val oldMatrixData = defaultGson.fromJson(json, OldMatrixData::class.java)!!
                    return oldMatrixData.getData()
                }
            }

            return null
        }
    }

    private class LayersInfoDeserializer : JsonDeserializer<LayerInfo?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): LayerInfo? {
            json ?: return null

            if (json is JsonObject) {
                if (json.has("layerId") && !json.has("id") && run {
                        val layerId = json.get("layerId")
                        layerId.isJsonPrimitive && layerId.asJsonPrimitive.isNumber
                    }) {

                    json.add("id", json.get("layerId"))
                    json.remove("layerId")
                }
                return defaultGson.fromJson(json, LayerInfo::class.java)
            }
            return null
        }
    }

    private class OldSavedColorDeserializer : JsonDeserializer<SavedColor?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): SavedColor? {
            json ?: return null

            if (json is JsonObject) {
                if (json.has("colorHSVA") && json.has("colorName")) {
                    val hsva = json.get("colorHSVA")
                    if (hsva !is JsonArray || !hsva.all { it.isJsonPrimitive }) return null
                    val name = json.get("colorName")
                    if (name !is JsonPrimitive || !name.isString) return null
                    if (!hsva.all { it.asJsonPrimitive.isNumber }) return null

                    return SavedColor(hsva.take(3).map { it.asFloat }.toFloatArray(), hsva[3].asInt, name.asString)
                }

                return defaultGson.fromJson(json, SavedColor::class.java)
            }

            return null
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
        fun getData() = floatArrayOf(
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
    }
}
