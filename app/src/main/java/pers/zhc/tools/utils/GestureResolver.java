package pers.zhc.tools.utils;

import android.content.Context;
import android.view.MotionEvent;

public class GestureResolver {
    private GestureInterface gestureInterface;

    public GestureResolver(Context context, GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private float lastX = -1, lastY = -1;

    public void onTouch(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            float x1 = event.getX(0);
            float y1 = event.getY(0);
            float x2 = event.getX(1);
            float y2 = event.getY(1);
            float midX = (x1 + x2) / 2;
            float midY = (y1 + y2) / 2;
            if (lastX == -1 && lastY == -1) {
                lastX = midX;
                lastY = midY;
            } else
                this.gestureInterface.onTwoPointScroll(midX - lastX, midY - lastY, event);
            this.lastX = midX;
            this.lastY = midY;
            /*float distance = getDistance(x1, x2, y1, y2);
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
            }*/
        } else {
            lastX = -1;
            lastY = -1;
        }
    }

    public interface GestureInterface {
        /**
         * 两个点的移动
         *
         * @param distanceX   x方向的变化（与上一次最后一次触摸的x距离）
         * @param distanceY   y方向的变化（与上一次最后一次触摸的y距离）
         * @param motionEvent 事件
         */
        void onTwoPointScroll(float distanceX, float distanceY, MotionEvent motionEvent);

    }
}