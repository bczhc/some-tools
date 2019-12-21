package pers.zhc.tools.test.viewtest;

import android.graphics.Path;

/**
 * Author: aaa
 * Date: 2016/10/15 11:24.
 * 当前涂鸦的路径
 */
public class MyDrawSinglePath {
    private MyPen mMyPen;
    private Path mPath;

    public MyDrawSinglePath(int color, int size, int alpha, boolean b){
        mMyPen = new MyPen(color, size, alpha, b);
        mPath = new Path();
    }

    public MyPen getMyPen() {
        return mMyPen;
    }

    public Path getPath() {
        return mPath;
    }
}