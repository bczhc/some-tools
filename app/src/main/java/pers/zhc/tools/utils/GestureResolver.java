package pers.zhc.tools.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public abstract class GestureResolver {
    private GestureInterface gestureInterface;
    private GestureDetector gestureDetector;
    private float pDiatanceSum, lastPDistanceSum;

    public GestureResolver(Context context, GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
        gestureDetector = new GestureDetector(context, this.gestureInterface);
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public void onTouch(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        int pointerCount = event.getPointerCount();
        if (pointerCount > 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }
    }

    public void setGestureInterface(GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
    }

    public interface GestureInterface extends GestureDetector.OnGestureListener {
        void onZoomGesture(float firstDistance, float lastDistance, float scale, MotionEvent e);
    }
}