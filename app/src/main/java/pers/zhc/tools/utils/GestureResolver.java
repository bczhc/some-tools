package pers.zhc.tools.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureResolver {
    private GestureInterface gestureInterface;
    private GestureDetector gestureDetector;
    private float firstDistance;

    public GestureResolver(Context context, GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
        gestureDetector = new GestureDetector(context, this.gestureInterface);
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private float lastScale = 1;

    public void onTouch(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            float x1 = event.getX(0);
            float x2 = event.getX(1);
            float y1 = event.getY(0);
            float y2 = event.getY(1);
            float distance = getDistance(x1, x2, y1, y2);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (firstDistance == 0.0) firstDistance = distance;
                    float currentScale = distance / firstDistance;
                    float scaleC = distance / firstDistance;
                    firstDistance = distance;
                    gestureInterface.onZoomGesture(firstDistance
                            , distance, currentScale, scaleC * lastScale, (x1 + x2) / 2, (y1 + y2) / 2, event);
                    break;
                case MotionEvent.ACTION_UP:
                    firstDistance = 0;
                    break;
            }
        } else firstDistance = 0;
        gestureDetector.onTouchEvent(event);
    }

    public void setGestureInterface(GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
    }

    public interface GestureInterface extends GestureDetector.OnGestureListener {
        void onZoomGesture(float pDistance, float distance, float currentScale, float scaleC, float centralPointX, float centralPointY, MotionEvent event);
    }
}