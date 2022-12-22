package pers.zhc.tools.fdb;

import android.graphics.*;
import pers.zhc.tools.floatingdrawing.PaintView;

import java.util.ArrayList;
import java.util.LinkedList;

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

    public long getId() {
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
}
