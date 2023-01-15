package pers.zhc.tools.fdb;

import android.graphics.*;
import kotlin.text.Regex;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.floatingdrawing.PaintView;
import pers.zhc.tools.utils.RegexUtilsKt;
import pers.zhc.tools.utils.UUID;

import java.util.ArrayList;

/**
 * @author bczhc
 */
public class Layer {
    private LayerInfo layerInfo;
    public ArrayList<PaintView.PathBean> undoList = new ArrayList<>();
    public ArrayList<PaintView.PathBean> redoList = new ArrayList<>();
    public Bitmap bitmap;

    public Layer(int width, int height, LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
        updateBitmap(width, height);
    }

    public void updateBitmap(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void redrawBitmap(Matrix matrix) {
        Canvas canvas = new Canvas(bitmap);
        // clear the bitmap
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.setMatrix(matrix);
        for (PaintView.PathBean pathBean : undoList) {
            canvas.drawPath(pathBean.path, pathBean.paint);
        }
    }

    public String getId() {
        return layerInfo.getId();
    }

    public boolean isVisible() {
        return layerInfo.getVisible();
    }

    public void setVisible(boolean visible) {
        layerInfo.setVisible(visible);
    }

    public void setLayerInfo(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    public LayerInfo getLayerInfo() {
        return layerInfo;
    }

    public static @NotNull String randomId() {
        return UUID.INSTANCE.randomNoDash();
    }

    @Contract(pure = true)
    public static boolean checkTableName(@NotNull String table) {
        return table.startsWith("path_layer_");
    }

    public static String getTableLayerId(String table) {
        return RegexUtilsKt.capture(table, new Regex("^path_layer_(.*)$")).get(0).get(1);
    }
}
