package pers.zhc.tools.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureResolver {
    private GestureInterface gestureInterface;
    private GestureDetector gestureDetector;
    private float firstDistance = -1;
    /**
     * 可变的距离，最初是firstDistance，后面会等于上一次的距离，dScale用
     */
    private float tDistance = -1;

    public GestureResolver(Context context, GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
        gestureDetector = new GestureDetector(context, this.gestureInterface);
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

//    private float lastScale = 1;

    public class Point {
        public float x, y;
        private boolean first = true;
    }

    public void onTouch(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        float x1 = 0;
        float x2 = 0;
        float y1 = 0;
        float y2 = 0;
        float currentDistance = getDistance(x1, x2, y1, y2);
        if (event.getPointerCount() == 2) {
            x1 = event.getX(0);
            x2 = event.getX(1);
            y1 = event.getY(0);
            y2 = event.getY(1);
            currentDistance = getDistance(x1, x2, y1, y2);
        }
        Point firstMidPoint = new Point();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /*if (event.getPointerCount() == 2) {
                    this.firstDistance = getDistance(x1, x2, y1, y2);
                    this.tDistance = this.firstDistance;
                }*/
                break;
            case MotionEvent.ACTION_MOVE:
                if (firstMidPoint.first) {
                    firstMidPoint.x = (x1 + x2) / 2F;
                    firstMidPoint.y = (y1 + y2) / 2F;
                }
                firstMidPoint.first = false;
                if (event.getPointerCount() == 2) {
                    if (this.firstDistance == -1) this.firstDistance = getDistance(x1, x2, y1, y2);
                    if (this.tDistance == -1) this.tDistance = this.firstDistance;
                    gestureInterface.onZoomGesture(firstDistance, currentDistance
                            , currentDistance / firstDistance
                            , currentDistance / tDistance
                            , (x1 + x2) / 2F, (y1 + y2) / 2F
                            , firstMidPoint
                            , event);
                    this.tDistance = getDistance(x1, x2, y1, y2);
                }
                break;
            case MotionEvent.ACTION_UP:
                firstDistance = -1;
                tDistance = -1;
                firstMidPoint.first = true;
                break;
        }
    }

    public interface GestureInterface extends GestureDetector.OnGestureListener {
        //        void onZoomGesture(float pDistance, float distance, float currentScale, float scaleC, float centralPointX, float centralPointY, MotionEvent event);
        void onZoomGesture(float firstDistance, float currentDistance, float currentScale, float dScale, float midPointX, float midPointY, Point firstMidPoint, MotionEvent event);
        /*
        original * scale * (1 / scale)
        ... * scale2 * (1 / scale2)
         */
    }
}