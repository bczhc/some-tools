package pers.zhc.tools.fdb;

import android.graphics.Matrix;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.views.HSVAColorPickerRL;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Path saver.
 *
 * <h3>Database structure:</h3>
 * <ol>
 *     <li>mark INTEGER</li>
 *     <li>info BLOB</li>
 *     <li>x REAL</li>
 *     <li>y REAL</li>
 * </ol>
 *
 * <ul>
 *     <li>
 *         drawing:<br/>
 *         let action = {@link MotionEvent#getAction()}<br/>
 *         if action is:
 *         <ul>
 *             <li>
 *                 {@link MotionEvent#ACTION_DOWN}:
 *                 <ul>
 *                     <li>mark: {@code 0x01}</li>
 *                     <li>info: some infos about the stroke</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_MOVE}:
 *                 <ul>
 *                     <li>mark: {@code 0x02}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_UP}
 *                 <ul>
 *                     <li>mark: {@code 0x03}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         erasing:<br/>
 *         let action = {@link MotionEvent#getAction()}<br/>
 *         if action is:
 *         <ul>
 *             <li>
 *                 {@link MotionEvent#ACTION_DOWN}:
 *                 <ul>
 *                     <li>mark: {@code 0x11}</li>
 *                     <li>info: some infos about the stroke</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_MOVE}:
 *                 <ul>
 *                     <li>mark: {@code 0x12}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_UP}
 *                 <ul>
 *                     <li>mark: {@code 0x13}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         Undo:
 *         <ul>
 *             <li>mark: {@code 0x20}</li>
 *             <li>info: NULL</li>
 *             <li>x: NULL</li>
 *             <li>y: NULL</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Redo:
 *         <ul>
 *             <li>mark: {@code 0x30}</li>
 *             <li>info: NULL</li>
 *             <li>x: NULL</li>
 *             <li>y: NULL</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class PathSaver {
    private final SQLite3 pathDatabase;
    private final ArrayList<LayerPathSaver> layerPathSaverList = new ArrayList<>();

    public static final String PATH_VERSION = "3.1";

    public PathSaver(String path) {
        this.pathDatabase = SQLite3.open(path);

        configureDatabase();

        beginTransaction();
    }

    private void configureDatabase() {
        // create table
        pathDatabase.exec("CREATE TABLE IF NOT EXISTS info\n" +
                "(\n" +
                "    version          TEXT NOT NULL,\n" +
                "    create_timestamp INTEGER,\n" +
                "    extra_infos      TEXT NOT NULL\n" +
                ")");

        Statement infoStatement = pathDatabase.compileStatement("INSERT INTO info VALUES(?,?,?)");
        infoStatement.reset();
        infoStatement.bindText(1, PATH_VERSION);
        infoStatement.bind(2, System.currentTimeMillis());
        infoStatement.bindText(3, "");
        infoStatement.step();
        infoStatement.release();
    }

    public void addNewLayerPathSaver(long id) {
        final LayerPathSaver layerPathSaver = new LayerPathSaver(pathDatabase, id);
        layerPathSaverList.add(layerPathSaver);
    }

    public void setExtraInfos(@NotNull ExtraInfos extraInfos) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isLockingStroke", extraInfos.isLockingStroke());
            jsonObject.put("lockedDrawingStrokeWidth", extraInfos.getLockedDrawingStrokeWidth());
            jsonObject.put("lockedEraserStrokeWidth", extraInfos.getLockedEraserStrokeWidth());

            JSONArray savedColorsJSONArray = new JSONArray();
            for (HSVAColorPickerRL.SavedColor savedColor : extraInfos.getSavedColors()) {
                JSONObject savedColorJSONObject = new JSONObject();

                JSONArray hsvaJSONOArray = new JSONArray();
                hsvaJSONOArray.put(savedColor.hsv[0]);
                hsvaJSONOArray.put(savedColor.hsv[1]);
                hsvaJSONOArray.put(savedColor.hsv[2]);
                hsvaJSONOArray.put(savedColor.alpha);

                savedColorJSONObject.put("colorHSVA", hsvaJSONOArray);
                savedColorJSONObject.put("colorName", savedColor.name);
                savedColorsJSONArray.put(savedColorJSONObject);
            }
            jsonObject.put("savedColors", savedColorsJSONArray);

            float[] matrixValues = new float[9];
            extraInfos.getDefaultTransformation().getValues(matrixValues);
            JSONObject defaultTransformationJSONObject = new JSONObject();
            defaultTransformationJSONObject.put("MSCALE_X", matrixValues[Matrix.MSCALE_X]);
            defaultTransformationJSONObject.put("MSKEW_X", matrixValues[Matrix.MSKEW_X]);
            defaultTransformationJSONObject.put("MTRANS_X", matrixValues[Matrix.MTRANS_X]);
            defaultTransformationJSONObject.put("MSKEW_Y", matrixValues[Matrix.MSKEW_Y]);
            defaultTransformationJSONObject.put("MSCALE_Y", matrixValues[Matrix.MSCALE_Y]);
            defaultTransformationJSONObject.put("MTRANS_Y", matrixValues[Matrix.MTRANS_Y]);
            defaultTransformationJSONObject.put("MPERSP_0", matrixValues[Matrix.MPERSP_0]);
            defaultTransformationJSONObject.put("MPERSP_1", matrixValues[Matrix.MPERSP_1]);
            defaultTransformationJSONObject.put("MPERSP_2", matrixValues[Matrix.MPERSP_2]);
            jsonObject.put("defaultTransformation", defaultTransformationJSONObject);

            JSONArray layersInfoJSONArray = new JSONArray();
            final List<LayerInfo> layersInfo = extraInfos.getLayersInfo();
            for (LayerInfo layerInfo : layersInfo) {
                JSONObject layerInfoJSONObject = new JSONObject();
                layerInfoJSONObject.put("id", layerInfo.getLayerId());
                layerInfoJSONObject.put("name", layerInfo.getName());
                layerInfoJSONObject.put("visible", layerInfo.getVisible());
                layersInfoJSONArray.put(layerInfoJSONObject);
            }
            jsonObject.put("layersInfo", layersInfoJSONArray);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        String extraString = jsonObject.toString();

        Statement infoStatement = pathDatabase.compileStatement("UPDATE info\n" +
                "SET extra_infos = ?");
        infoStatement.bindText(1, extraString);
        infoStatement.step();
        infoStatement.release();
    }

    public void flush() {
        pathDatabase.exec("COMMIT");
        pathDatabase.exec("BEGIN TRANSACTION");
    }

    public void reset() {
        pathDatabase.exec("DROP TABLE IF EXISTS info");
        for (LayerPathSaver layerPathSaver : layerPathSaverList) {
            layerPathSaver.reset();
        }
        configureDatabase();
    }

    public void close() {
        pathDatabase.commit();
        pathDatabase.close();
    }

    public void commit() {
        pathDatabase.commit();
    }

    public void beginTransaction() {
        pathDatabase.beginTransaction();
    }

    @Nullable
    public static JSONObject getExtraInfos(@NotNull SQLite3 db) {
        String jsonString = null;

        final Statement statement = db.compileStatement("SELECT extra_infos FROM info");
        final Cursor cursor = statement.getCursor();

        if (cursor.step()) {
            jsonString = cursor.getText(0);
        }

        statement.release();

        if (jsonString == null) {
            return null;
        }

        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @org.jetbrains.annotations.Nullable
    @Nullable
    public LayerPathSaver getLayerPathSaver(long id) {
        for (LayerPathSaver layerPathSaver : layerPathSaverList) {
            if (layerPathSaver.getLayerId() == id) {
                return layerPathSaver;
            }
        }
        return null;
    }

    public String getDatabasePath() {
        return pathDatabase.getDatabasePath();
    }

    public File save() {
        flush();
        return new File(pathDatabase.getDatabasePath());
    }
}
