package pers.zhc.tools.floatingdrawing;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import pers.zhc.tools.views.AbstractHSVAColorPickerRelativeLayout;

public class FloatingViewOnTouchListener implements View.OnTouchListener {
    final private WindowManager.LayoutParams layoutParams;
    private final int width;
    private final int height;
    private final WindowManager wm;
    private final View view;
    private final ViewSpec viewSpec;
    private int lastRawX, lastRawY, paramX, paramY;

    public FloatingViewOnTouchListener(WindowManager.LayoutParams WM_layoutParams, WindowManager windowManager
            , View view, int width, int height, ViewSpec viewSpec) {
        this.layoutParams = WM_layoutParams;
        this.wm = windowManager;
        this.width = width;
        this.height = height;
        this.view = view;
        this.viewSpec = viewSpec;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastRawX = (int) rawX;
                lastRawY = (int) rawY;
                paramX = layoutParams.x;
                paramY = layoutParams.y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) rawX - lastRawX;
                int dy = (int) rawY - lastRawY;
                int lp2_x = paramX + dx;
                int lp2_y = paramY + dy;
                int constrainX, constrainY;
                constrainX = lp2_x - AbstractHSVAColorPickerRelativeLayout.limitValue(lp2_x, ((int) (-width / 2F + viewSpec.width / 2F)), ((int) (width / 2F - viewSpec.width / 2F)));
                constrainY = lp2_y - AbstractHSVAColorPickerRelativeLayout.limitValue(lp2_y, ((int) (-height / 2F + viewSpec.height / 2F)), ((int) (height / 2F - viewSpec.height / 2F)));
                if (constrainX == 0) {
                    layoutParams.x = lp2_x;
                } else {
                    paramX -= constrainX;
                }
                if (constrainY == 0) {
                    layoutParams.y = lp2_y;
                } else {
                    paramY -= constrainY;
                }
                // 更新悬浮窗位置
                wm.updateViewLayout(view, layoutParams);
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(lastRawX - rawX) < 1 && Math.abs(lastRawY - rawY) < 1) {
                    v.performClick();
                }
                break;
        }
        return true;
    }

    public static class ViewSpec {
        int width = 0, height = 0;

        public ViewSpec(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public ViewSpec() {
        }
    }
}
