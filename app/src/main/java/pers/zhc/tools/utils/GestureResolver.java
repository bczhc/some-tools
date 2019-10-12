package pers.zhc.tools.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GestureResolver {
    private GestureInterface gestureInterface;
    private GestureDetector gestureDetector;
    private float pDistance;
    private ScaleGestureDetector scaleGestureDetector;

    public GestureResolver(Context context, GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
        gestureDetector = new GestureDetector(context, this.gestureInterface);
        scaleGestureDetector = new ScaleGestureDetector(context, this.gestureInterface);
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private float lastScale = 1;
    private float scaleC = 1;

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
                    if (pDistance == 0.0) pDistance = distance;
                    float currentScale = distance / pDistance;
                    if (currentScale != lastScale) {
                        scaleC *= currentScale;
                        lastScale = currentScale;
                    }
                    gestureInterface.onZoomGesture(pDistance
                            , distance, currentScale, scaleC, (x1 + x2) / 2, (y1 + y2) / 2, event);
                    break;
                case MotionEvent.ACTION_UP:
                    pDistance = 0;
                    break;
            }
        }
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
    }

    public void setGestureInterface(GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
    }

    public static class GestureInterface extends ScaleGestureDetector.SimpleOnScaleGestureListener implements GestureDetector.OnGestureListener {
        void onZoomGesture(float firstDistance, float currentDistance, float scale, float scaleC, float centralPointX, float centralPointY, MotionEvent e) {

        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}