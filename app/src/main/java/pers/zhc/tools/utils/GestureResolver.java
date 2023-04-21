package pers.zhc.tools.utils;

import android.view.MotionEvent;

public class GestureResolver {
    private final GestureInterface gestureInterface;
    private float lastX = -1, lastY = -1;
    private float lastDistance = -1;
    private float firstDistance = -1;
    private float firstMidPointX;
    private float firstMidPointY;
    private boolean twoPointsDown = false;
    private float lastOnePointX = -1, lastOnePointY = -1;

    private float lastX1 = -1, lastY1 = -1, lastX2 = -1, lastY2 = -1;
    private boolean firstTwoDown = true;

    public GestureResolver(GestureInterface gestureInterface) {
        this.gestureInterface = gestureInterface;
    }

    private float getDistance(float x1, float x2, float y1, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public void onTouch(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            float x1 = event.getX(0);
            float y1 = event.getY(0);
            float x2 = event.getX(1);
            float y2 = event.getY(1);

            if (firstTwoDown) {
                lastX1 = x1;
                lastY1 = y1;
                lastX2 = x2;
                lastY2 = y2;
                firstTwoDown = false;
            }

            if (!twoPointsDown) {
                gestureInterface.onTwoPointsDown(event);
            }
            twoPointsDown = true;
            gestureInterface.onTwoPointsPress(event);

            {
                float midX = (x1 + x2) / 2;
                float midY = (y1 + y2) / 2;
                if (lastX == -1 && lastY == -1) {
                    lastX = midX;
                    lastY = midY;
                } else {
                    this.gestureInterface.onTwoPointsScroll(midX - lastX, midY - lastY, event);
                }
                this.lastX = midX;
                this.lastY = midY;
            }
            {
                float distance = getDistance(x1, x2, y1, y2);
                if (firstDistance == -1) {
                    firstDistance = distance;
                    firstMidPointX = (x1 + x2) / 2;
                    firstMidPointY = (y1 + y2) / 2;
                }
                if (lastDistance == -1) {
                    lastDistance = distance;
                }
                float midPointX, midPointY;
                midPointX = (x1 + x2) / 2;
                midPointY = (y1 + y2) / 2;
                this.gestureInterface.onTwoPointsZoom(firstMidPointX, firstMidPointY, midPointX, midPointY, firstDistance, distance, distance / firstDistance, distance / lastDistance, event);
                lastDistance = distance;
            }

            // two points rotate
            float lastMidX = (lastX1 + lastX2) / 2F;
            float lastMidY = (lastY1 + lastY2) / 2F;
            float midX = (x1 + x2) / 2F;
            float midY = (y1 + y2) / 2F;
            final float degrees = getVectorDegrees(lastX1 - lastMidX, lastY1 - lastMidY, x1 - midX, y1 - midY);
            gestureInterface.onTwoPointsRotate(event, firstMidPointX, firstMidPointY, degrees, midX, midY);

            lastX1 = x1;
            lastY1 = y1;
            lastX2 = x2;
            lastY2 = y2;
        } else {
            if (twoPointsDown) {
                twoPointsDown = false;
                lastX = -1;
                lastY = -1;
                firstDistance = -1;
                lastDistance = -1;
                firstTwoDown = true;
                gestureInterface.onTwoPointsUp(event);
            }
        }
        if (pointerCount == 1) {
            float x = event.getX();
            float y = event.getY();
            if (lastOnePointX == -1 && lastOnePointY == -1) {
                lastOnePointX = x;
                lastOnePointY = y;
            } else {
                this.gestureInterface.onOnePointScroll(x - lastOnePointX, y - lastOnePointY, event);
            }
            this.lastOnePointX = x;
            this.lastOnePointY = y;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                lastOnePointX = -1;
                lastOnePointY = -1;
            }
        } else {
            lastOnePointX = -1;
            lastOnePointY = -1;
        }
    }

    private float getVectorDegrees(float x1, float y1, float x2, float y2) {
        return (float) ((Math.atan2(y2, x2) - Math.atan2(y1, x1)) / Math.PI * 180D);
    }

    public interface GestureInterface {
        /**
         * 两个点的移动
         *
         * @param distanceX x方向的变化（与上一次触摸的x距离）
         * @param distanceY y方向的变化（与上一次触摸的y距离）
         * @param event     事件
         */
        void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event);

        /**
         * 两个点的缩放
         *
         * @param firstMidPointX 最开始中间点x
         * @param firstMidPointY 最开始中间点y
         * @param midPointX      当前中间点x
         * @param midPointY      当前中间点y
         * @param firstDistance  最开始两点之间的距离
         * @param distance       现在的距离
         * @param scale          与最开始的缩放比例
         * @param dScale         与上一次触摸的缩放比例
         * @param event          事件
         */
        void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY,
                             float firstDistance, float distance, float scale, float dScale, MotionEvent event);

        /**
         * It will be invoked when two points change to one point or no point.
         *
         * @param event event
         */
        void onTwoPointsUp(MotionEvent event);

        /**
         * It will be invoked when two points press down the first time.
         *
         * @param event event
         */
        void onTwoPointsDown(MotionEvent event);

        /**
         * It will be invoked when two points press down.
         *
         * @param event event
         */
        void onTwoPointsPress(MotionEvent event);

        /**
         * Called when two points do rotation
         *
         * @param event     motion event
         * @param firstMidX the first mid point x
         * @param firstMidY the first mid point y
         * @param degrees   rotation degrees
         */
        void onTwoPointsRotate(MotionEvent event, float firstMidX, float firstMidY, float degrees, float midX, float midY);

        /**
         * 一个点的移动
         *
         * @param distanceX x方向的变化（与上一次触摸的x距离）
         * @param distanceY y方向的变化（与上一次触摸的y距离）
         * @param event     事件
         */
        @SuppressWarnings({"EmptyMethod"})
        void onOnePointScroll(float distanceX, float distanceY, MotionEvent event);
    }
}
