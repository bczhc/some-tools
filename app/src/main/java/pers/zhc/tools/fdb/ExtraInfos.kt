package pers.zhc.tools.fdb

import android.graphics.Matrix
import org.json.JSONException
import org.json.JSONObject

/**
 * @author bczhc
 */
class ExtraInfos {
    companion object {
        fun getDefaultTransformation(defaultTransformationJSONObject: JSONObject): Matrix? {
            try {
                val matrixValues = FloatArray(9)
                matrixValues[Matrix.MSCALE_X] = defaultTransformationJSONObject.getDouble("MSCALE_X").toFloat()
                matrixValues[Matrix.MSKEW_X] = defaultTransformationJSONObject.getDouble("MSKEW_X").toFloat()
                matrixValues[Matrix.MTRANS_X] = defaultTransformationJSONObject.getDouble("MTRANS_X").toFloat()
                matrixValues[Matrix.MSKEW_Y] = defaultTransformationJSONObject.getDouble("MSKEW_Y").toFloat()
                matrixValues[Matrix.MSCALE_Y] = defaultTransformationJSONObject.getDouble("MSCALE_Y").toFloat()
                matrixValues[Matrix.MTRANS_Y] = defaultTransformationJSONObject.getDouble("MTRANS_Y").toFloat()
                matrixValues[Matrix.MPERSP_0] = defaultTransformationJSONObject.getDouble("MPERSP_0").toFloat()
                matrixValues[Matrix.MPERSP_1] = defaultTransformationJSONObject.getDouble("MPERSP_1").toFloat()
                matrixValues[Matrix.MPERSP_2] = defaultTransformationJSONObject.getDouble("MPERSP_2").toFloat()
                val matrix = Matrix()
                matrix.setValues(matrixValues)
                return matrix
            } catch (ignored: JSONException) {
                return null
            }
        }
    }
}