package pers.zhc.tools.floatingboard;

//old:
//import android.annotation.SuppressLint;import android.content.Context;import android.graphics.Canvas;import android.graphics.LinearGradient;import android.graphics.Paint;import android.support.annotation.NonNull;import android.util.AttributeSet;import android.view.View;import android.view.ViewGroup;import android.widget.LinearLayout;import android.widget.RelativeLayout;class HSVColorPickerRL extends RelativeLayout { private int width = 0, height = 0;private Context context;HSVColorPickerRL(@NonNull Context context) { super(context); }public HSVColorPickerRL(Context context, AttributeSet attrs) { super(context, attrs);this.context = context; }private void init() { LinearLayout ll = new LinearLayout(context);ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));ll.setOrientation(LinearLayout.VERTICAL);HView hView = new HView(context);LinearLayout[] hsvLLs = new LinearLayout[3];LinearLayout.LayoutParams hsvLL_LP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);for (int i = 0; i < hsvLLs.length; i++) { hsvLLs[i] = new LinearLayout(context);hsvLLs[i].setLayoutParams(hsvLL_LP); }hsvLLs[0].addView(hView);this.addView(ll); }@Override protected void onLayout(boolean changed, int l, int t, int r, int b) { super.onLayout(changed, l, t, r, b);width = r - l;height = b - t;init(); }class HView extends View { private int hW, hH;private Paint hPaint;private int[] hColors;public HView(Context context) { super(context); }public HView(Context context, AttributeSet attrs) { super(context, attrs); }private void hInit() { hPaint = new Paint();hColors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};invalidate(); }@Override protected void onDraw(Canvas canvas) {/*            super.onDraw(canvas);*/@SuppressLint("DrawAllocation") LinearGradient lg = new LinearGradient(0F, 0F, hW, hH, hColors, null, LinearGradient.TileMode.CLAMP);hPaint.setShader(lg);canvas.drawRect(0,0,hW,hH,hPaint); }@Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) { super.onLayout(changed, left, top, right, bottom);hW = right - left;hH = top - bottom;hInit(); }}}

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.R;

import static pers.zhc.tools.utils.ColorUtils.invertColor;

@SuppressLint("ViewConstructor")
abstract class HSVColorPickerRL extends RelativeLayout {
    private float[] hsv = new float[3];
    private int alpha;
    private int width, height;
    private Context context;
    private View[] hsvAView;
    private View vv;
    private float[][] temp = new float[4][4];
    private float[] currentXPos = new float[4];
    private Paint oppositeColorPaint;
    private float lW = 3;

    HSVColorPickerRL(Context context, int initializeColor, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
        this.context = context;
        oppositeColorPaint = new Paint();
        alpha = initializeColor >>> 24;
        Color.colorToHSV(initializeColor, hsv);
        for (int i = 0; i < 4; i++) {
            setCurrentX(i);
        }
        init();
    }

    private void init() {
        int perViewHeight = height / 4;
        hsvAView = new View[]{
                new HView(context, width, perViewHeight),
                new SView(context, width, perViewHeight),
                new VView(context, width, perViewHeight),
                new AView(context, width, perViewHeight)
        };
        LinearLayout ll = new LinearLayout(context);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(width, height);
        ll.setLayoutParams(ll_lp);
        ll.setOrientation(LinearLayout.VERTICAL);
        LinearLayout barLL = new LinearLayout(context);
        barLL.setLayoutParams(new LinearLayout.LayoutParams(width, height / 12));
        barLL.setOrientation(LinearLayout.HORIZONTAL);
        TextView tv = new TextView(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        } else tv.setTextSize(25F);
        tv.setText(R.string.h_s_v_a_color_picker);
        tv.setBackgroundColor(Color.WHITE);
        tv.setWidth(width / 2);
        tv.setHeight(height / 12);
        barLL.addView(tv);
        vv = new View(context);
        vv.setLayoutParams(new ViewGroup.LayoutParams(width / 2, ViewGroup.LayoutParams.MATCH_PARENT));
        vv.setBackgroundColor(Color.HSVToColor(alpha, hsv));
        barLL.addView(vv);
        ll.addView(barLL);
        LinearLayout[] linearLayouts = new LinearLayout[hsvAView.length];
        for (int i = 0; i < linearLayouts.length; i++) {
            linearLayouts[i] = new LinearLayout(context);
            linearLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(width, 0, 1));
            linearLayouts[i].addView(hsvAView[i]);
            ll.addView(linearLayouts[i]);
        }
        this.addView(ll);
    }

    private class HView extends View {
        private int hW, hH;
        private Paint hPaint;

        HView(Context context, int width, int height) {
            super(context);
            this.hW = width;
            this.hH = height;
            hInit();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //        super.onDraw(canvas);
            System.arraycopy(hsv, 0, temp[0], 0, hsv.length);
            temp[0][3] = alpha;
            for (float i = 0; i < hW; i++) {
                temp[0][0] = i * 360F / ((float) hW);
                hPaint.setColor(Color.HSVToColor((int) temp[0][3], temp[0]));
                canvas.drawLine(i, 0, i, hH, hPaint);
            }
            canvas.drawRect(setCurrentX(0), 0, currentXPos[0] + lW, hH, oppositeColorPaint);
        }

        private void hInit() {
            hPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(hW, hH);
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            hsv[0] = event.getX() * 360F / ((float) hW);
            oppositeColorPaint.setColor(invertColor(Color.HSVToColor(255, hsv)));
//            System.out.println("color = " + color);
            invalidateAllView();
            return true;
        }
    }

    private class SView extends View {
        private int sW, sH;
        private Paint sPaint;

        SView(Context context, int sW, int sH) {
            super(context);
            this.sW = sW;
            this.sH = sH;
            sInit();
        }

        private void sInit() {
            sPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(sW, sH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
            System.arraycopy(hsv, 0, temp[1], 0, hsv.length);
            temp[1][3] = alpha;
            for (float i = 0; i < sW; i++) {
                temp[1][1] = i / ((float) sW);
                sPaint.setColor(Color.HSVToColor((int) temp[1][3], temp[1]));
                canvas.drawLine(i, 0F, i, ((float) height), sPaint);
            }
            canvas.drawRect(setCurrentX(1), 0F, currentXPos[1] + lW, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            hsv[1] = event.getX() / ((float) sW);
            oppositeColorPaint.setColor(invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    private class VView extends View {
        private int vW, vH;
        private Paint vPaint;

        VView(Context context, int vW, int vH) {
            super(context);
            this.vW = vW;
            this.vH = vH;
            vInit();
        }

        private void vInit() {
            vPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(vW, vH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
            System.arraycopy(hsv, 0, temp[2], 0, hsv.length);
            temp[2][3] = alpha;
            for (float i = 0; i < vW; i++) {
                temp[2][2] = i / ((float) vW);
                vPaint.setColor(Color.HSVToColor((int) temp[2][3], temp[2]));
                canvas.drawLine(i, 0F, i, ((float) height), vPaint);
            }
            canvas.drawRect(setCurrentX(2), 0F, currentXPos[2] + lW, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            hsv[2] = event.getX() / ((float) vW);
            oppositeColorPaint.setColor(invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    private class AView extends View {
        private int aW, aH;
        private Paint aPaint;

        AView(Context context, int aW, int aH) {
            super(context);
            this.aW = aW;
            this.aH = aH;
            aInit();
        }

        private void aInit() {
            aPaint = new Paint();
        }

        @Override
        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
            System.arraycopy(hsv, 0, temp[3], 0, hsv.length);
            temp[3][3] = alpha;
            for (float i = 0; i < aW; i++) {
                aPaint.setColor(Color.HSVToColor((int) (i * 255 / ((float) aW)), temp[3]));
                canvas.drawLine(i, 0F, i, ((float) aH), aPaint);
            }
            canvas.drawRect(setCurrentX(3), 0F, currentXPos[3] + lW, aH, oppositeColorPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(aW, aH);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            alpha = (int) (event.getX() / ((float) aW) * 255);
            alpha = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
            oppositeColorPaint.setColor(invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    private void invalidateAllView() {
        for (View view : hsvAView) {
//            if (i == notDrawIndex) continue;
//            int finalI = i;
//            new Thread(() -> hsvAView[finalI].postInvalidate()).start();
            view.invalidate();
        }
        int color = Color.HSVToColor(alpha, hsv);
        vv.setBackgroundColor(color);
        onPickedAction(color);
    }

    private float setCurrentX(int i) {
        if (i == 1 || i == 2) return currentXPos[i] = hsv[i] * width;
        if (i == 0) return currentXPos[0] = hsv[0] * ((float) width) / 360F;
        if (i == 3) return currentXPos[3] = alpha * ((float) width) / 255F;
        return 0;
    }

    abstract void onPickedAction(int color);
}