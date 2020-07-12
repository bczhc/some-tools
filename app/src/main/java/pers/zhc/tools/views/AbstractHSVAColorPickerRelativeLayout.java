package pers.zhc.tools.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.Selection;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ColorUtils;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.DisplayUtil;
import pers.zhc.tools.utils.ToastUtils;


@SuppressWarnings("SameParameterValue")
@SuppressLint("ViewConstructor")
public abstract class AbstractHSVAColorPickerRelativeLayout extends RelativeLayout {
    private final float[] hsv = new float[3];
    private final int width;
    private final int height;
    private final Context context;
    private final float[][] temp = new float[4][4];
    private final float[] currentXPos = new float[4];
    private final Paint oppositeColorPaint;
    private final float lW = 1.5F;
    private final float[] resultHSVA = new float[4];
    private int alpha;
    private View[] hsvaViews;
    private View vv;

    /**
     * @param context      ctx
     * @param initialColor initialColor
     * @param width        w
     * @param height       h
     * @param hsva         HSVA
     * @param dialog       防止dim
     */
    protected AbstractHSVAColorPickerRelativeLayout(Context context, int initialColor, int width, int height, @Nullable float[] hsva, @Nullable Dialog dialog) {
        super(context);
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.dimAmount = 0F;
                window.setAttributes(attributes);
            }
        }
        this.width = width;
        this.height = height;
        this.context = context;
        oppositeColorPaint = new Paint();
        if (hsva == null) {
            alpha = initialColor >>> 24;
            Color.colorToHSV(initialColor, hsv);
        } else {
            this.alpha = (int) hsva[3];
            System.arraycopy(hsva, 0, this.hsv, 0, 3);
        }
        for (int i = 0; i < 4; i++) {
            setCurrentX(i);
        }
        init();
    }

    public static int limitValue(int value, int min, int max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static float limitValue(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    private void init() {
        int perViewHeight = height / 4;
        hsvaViews = new View[]{
                new HView(context, width, perViewHeight),
                new SView(context, width, perViewHeight),
                new VView(context, width, perViewHeight),
                new AView(context, width, perViewHeight)
        };
        LinearLayout ll = new LinearLayout(context);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(width, height);
        ll.setLayoutParams(ll_lp);
        ll.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout barRL = new RelativeLayout(context);
        barRL.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tv = new TextView(context);
        tv.setText(R.string.h_s_v_a_color_picker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            tv.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            tv.setAutoSizeTextTypeUniformWithConfiguration(1, 200, 1, TypedValue.COMPLEX_UNIT_SP);
        } else {
            float r = ((float) height) / 12;
            tv.setTextSize(DisplayUtil.px2sp(context, r));
        }
        tv.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setBackgroundColor(Color.WHITE);
        tv.setId(R.id.tv);
        barRL.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            EditText editText = new EditText(context);
            int color = this.getColor();
            String hexString = ColorUtils.getHexString(color, true);
            editText.setText(hexString);
            adb.setView(editText);
            adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                String s = editText.getText().toString();
                int parseColor;
                try {
                    parseColor = Color.parseColor(s);
                    Color.colorToHSV(parseColor, this.hsv);
                    this.alpha = Color.alpha(parseColor);
                    setInvertedColor(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.show(context, R.string.please_type_correct_value);
                }
            });
            adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
            });
            adb.setTitle(R.string.please_enter_color_hex);
            AlertDialog ad = adb.create();
            DialogUtil.setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
            ad.show();
            Selection.selectAll(editText.getText());
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(editText, ad);
        });
        barRL.addView(tv);
        vv = new View(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.tv);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.tv);
        vv.setLayoutParams(layoutParams);
        vv.setBackgroundColor(Color.HSVToColor(alpha, hsv));
        barRL.addView(vv);
        ll.addView(barRL);
        LinearLayout[] linearLayouts = new LinearLayout[hsvaViews.length];
        for (int i = 0; i < linearLayouts.length; i++) {
            linearLayouts[i] = new LinearLayout(context);
            linearLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(width, 0, 1));
            linearLayouts[i].addView(hsvaViews[i]);
            ll.addView(linearLayouts[i]);
        }
        this.addView(ll);
        setInvertedColor(false);
    }

    private void invalidateAllView() {
        for (View view : hsvaViews) {
//            if (i == notDrawIndex) continue;
//            int finalI = i;
//            new Thread(() -> hsvAView[finalI].postInvalidate()).start();
            view.invalidate();
        }
        int color = this.getColor();
        vv.setBackgroundColor(color);
        System.arraycopy(this.hsv, 0, resultHSVA, 0, 3);
        resultHSVA[3] = alpha;
        onPickedAction(color, resultHSVA);
    }

    private int getColor() {
        return Color.HSVToColor(this.alpha, this.hsv);
    }

    /**
     * @param i index
     * @return pos
     */
    private float setCurrentX(int i) {
        if (i == 0) {
            return currentXPos[0] = hsv[0] * ((float) width) / 360F;
        }
        if (i == 3) {
            return currentXPos[3] = alpha * ((float) width) / 255F;
        }
        return currentXPos[i] = hsv[i] * width;
    }

    protected abstract void onPickedAction(int color, float[] hsva);

    private void setInvertedColor(boolean invalidate) {
        oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(255, hsv)));
        if (invalidate) {
            invalidateAllView();
        }
    }

    private class HView extends BaseView {
        private final int hW;
        private final int hH;
        private Paint hPaint;

        HView(Context context, int width, int height) {
            super(context);
            this.hW = width;
            this.hH = height;
            hInit();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            System.arraycopy(hsv, 0, temp[0], 0, hsv.length);
            temp[0][3] = alpha;
            for (float i = 0; i < hW; i++) {
                temp[0][0] = i * 360F / ((float) hW);
                hPaint.setColor(Color.HSVToColor((int) temp[0][3], temp[0]));
                canvas.drawLine(i, 0, i, hH, hPaint);
            }
            canvas.drawRect(setCurrentX(0) - lW, 0, currentXPos[0] + lW, hH, oppositeColorPaint);
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
            hsv[0] = limitValue(event.getX() * 360F / ((float) hW), 0, 360);
            setInvertedColor(true);
            return true;
        }
    }

    private class SView extends BaseView {
        private final int sW;
        private final int sH;
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
            canvas.drawRect(setCurrentX(1) - lW, 0F, currentXPos[1] + lW, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            hsv[1] = limitValue(event.getX() / ((float) sW), 0, 1F);
            oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    private class VView extends BaseView {
        private final int vW;
        private final int vH;
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
            canvas.drawRect(setCurrentX(2) - lW, 0F, currentXPos[2] + lW, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            hsv[2] = limitValue(event.getX() / ((float) vW), 0, 1);
            oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    /*private static void turn2Position(Position dest, int color) {

    }*/

    private class AView extends BaseView {
        private final int aW;
        private final int aH;
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
            canvas.drawRect(setCurrentX(3) - lW, 0F, currentXPos[3] + lW, aH, oppositeColorPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(aW, aH);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            alpha = limitValue((int) (event.getX() / ((float) aW) * 255), 0, 255);
            oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(255, hsv)));
            invalidateAllView();
            return true;
        }
    }

    /*private void colorToThisPosition(Position dest, @ColorInt int color) {
        int alpha = Color.alpha(color);
        float[] c = new float[3];
        Color.colorToHSV(color, c);
        dest.positions[0] = c[0] * width / 360F;
        dest.positions[1] = c[1] * width;
        dest.positions[2] = c[2] * width;
        dest.positions[3] = alpha * width / 255F;
    }*/
}