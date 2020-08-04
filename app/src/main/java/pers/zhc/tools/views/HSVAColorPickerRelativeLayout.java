package pers.zhc.tools.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Selection;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
public abstract class HSVAColorPickerRelativeLayout extends RelativeLayout {
    private final float[] currentXPos = new float[4];
    private final float lW = 1.5F;
    private final float[] currentHSVA = new float[4];
    private Paint oppositeColorPaint;
    private int width;
    private int height;
    private Context context;
    private View[] hsvaViews;
    private View colorPreviewView;

    /**
     * @param context      context
     * @param initialColor the initial color
     * @param width        view width
     * @param height       view height
     */
    protected HSVAColorPickerRelativeLayout(Context context, int initialColor, int width, int height) {
        super(context);
        Color.colorToHSV(initialColor, currentHSVA);
        currentHSVA[3] = Color.alpha(initialColor) / 255F;
        init(context, width, height);
    }

    /**
     * @param context context
     * @param hsva    HSVA float array
     * @param width   view width
     * @param height  view height
     */

    protected HSVAColorPickerRelativeLayout(Context context, float[] hsva, int width, int height) {
        super(context);
        System.arraycopy(hsva, 0, currentHSVA, 0, hsva.length);
        init(context, width, height);
    }

    public static int limitValue(int value, int min, int max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static float limitValue(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    private void setCurrentX() {
        currentXPos[0] = currentHSVA[0] * width / (float) 360;
        currentXPos[1] = currentHSVA[1] * width;
        currentXPos[2] = currentHSVA[2] * width;
        currentXPos[3] = currentHSVA[3] * width;
    }

    private void init(Context context, int width, int height) {
        this.context = context;
        this.width = width;
        this.height = height;
        int perViewHeight = this.height * 2 / 11;
        oppositeColorPaint = new Paint();
        setCurrentX();
        hsvaViews = new View[]{
                new HView(context, width, perViewHeight),
                new SView(context, width, perViewHeight),
                new VView(context, width, perViewHeight),
                new AView(context, width, perViewHeight)
        };
        LinearLayout ll = new LinearLayout(this.context);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(this.width, this.height);
        ll.setLayoutParams(ll_lp);
        ll.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout barRL = new RelativeLayout(this.context);
        barRL.setLayoutParams(new LinearLayout.LayoutParams(this.width, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tv = new TextView(this.context);
        tv.setText(R.string.h_s_v_a_color_picker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setAutoSizeTextTypeUniformWithConfiguration(1, 200, 1, TypedValue.COMPLEX_UNIT_SP);
        } else {
            float r = ((float) this.height) / 12;
            tv.setTextSize(DisplayUtil.px2sp(this.context, r));
        }
        tv.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setBackgroundColor(Color.WHITE);
        tv.setId(R.id.tv);
        barRL.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this.context);
            EditText editText = new EditText(this.context);
            int color = this.getColor();
            String hexString = ColorUtils.getHexString(color, true);
            editText.setText(hexString);
            adb.setView(editText);
            adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                String s = editText.getText().toString();
                if (s.charAt(0) == '#') s = s.substring(1);
                if (s.length() == 6) s = "#FF" + s;
                else if (s.length() == 7) s = "#0" + s;
                try {
                    if (s.length() > 8) throw new Exception(this.context.getString(R.string.please_type_correct_value));
                    final int parsedColor = Color.parseColor(s);
                    Color.colorToHSV(parsedColor, currentHSVA);
                    currentHSVA[3] = Color.alpha(parsedColor) / 255F;
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.show(this.context, R.string.please_type_correct_value);
                }
            });
            adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
            });
            adb.setTitle(R.string.please_enter_color_hex);
            AlertDialog ad = adb.create();
            ad.setButton(AlertDialog.BUTTON_NEUTRAL, this.context.getString(R.string.save_color), (dialog, which) -> {

            });
            DialogUtil.setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
            ad.show();
            Selection.selectAll(editText.getText());
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(editText, ad);
        });
        barRL.addView(tv);
        colorPreviewView = new View(this.context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.tv);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.tv);
        colorPreviewView.setLayoutParams(layoutParams);
        barRL.addView(colorPreviewView);
        ll.addView(barRL);
        LinearLayout[] linearLayouts = new LinearLayout[hsvaViews.length];
        for (int i = 0; i < linearLayouts.length; i++) {
            linearLayouts[i] = new LinearLayout(this.context);
            linearLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(this.width, 0, 1));
            linearLayouts[i].addView(hsvaViews[i]);
            ll.addView(linearLayouts[i]);
        }
        this.addView(ll);
        colorPreviewView.setBackgroundColor(Color.HSVToColor(((int) (currentHSVA[3] * 255)), currentHSVA));
    }

    private void invalidateAllViews() {
        oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(((int) (currentHSVA[3] * 255)), currentHSVA)));
        for (View view : hsvaViews) {
            view.invalidate();
        }
        onColorPicked(currentHSVA[0], currentHSVA[1], currentHSVA[2], ((int) (currentHSVA[3] * 255)));
        colorPreviewView.setBackgroundColor(Color.HSVToColor(((int) (currentHSVA[3] * 255)), currentHSVA));
    }

    private int getColor() {
        return Color.HSVToColor(((int) (currentHSVA[3] * 255)), this.currentHSVA);
    }

    public abstract void onColorPicked(float h, float s, float v, int alpha);

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
            for (float i = 0; i < hW; i++) {
                hPaint.setColor(ColorUtils.HSVAtoColor(((int) (currentHSVA[3] * 255)), i * 360 / (float) hW, currentHSVA[1], currentHSVA[2]));
                canvas.drawLine(i, 0, i, hH, hPaint);
            }
            canvas.drawRect(currentXPos[0] - lW, 0, currentXPos[0] + lW, hH, oppositeColorPaint);
        }

        private void hInit() {
            hPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(hW, hH);
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSVA[0] = limitValue(x * 360F / ((float) hW), 0, 360);
            currentXPos[0] = x;
            invalidateAllViews();
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
            setMeasuredDimension(sW, sH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < sW; i++) {
                sPaint.setColor(ColorUtils.HSVAtoColor(((int) (currentHSVA[3] * 255)), currentHSVA[0], i / (float) sW, currentHSVA[2]));
                canvas.drawLine(i, 0F, i, ((float) height), sPaint);
            }
            canvas.drawRect(currentXPos[1] - lW, 0F, currentXPos[1] + lW, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSVA[1] = limitValue(x / ((float) sW), 0, 1F);
            currentXPos[1] = x;
            invalidateAllViews();
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
            setMeasuredDimension(vW, vH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < vW; i++) {
                vPaint.setColor(ColorUtils.HSVAtoColor(((int) (currentHSVA[3] * 255)), currentHSVA[0], currentHSVA[1], i / (float) vW));
                canvas.drawLine(i, 0F, i, ((float) height), vPaint);
            }
            canvas.drawRect(currentXPos[2] - lW, 0F, currentXPos[2] + lW, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSVA[2] = limitValue(x / ((float) vW), 0, 1);
            currentXPos[2] = x;
            invalidateAllViews();
            return true;
        }
    }

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
            for (float i = 0; i < aW; i++) {
                aPaint.setColor(Color.HSVToColor((int) (i * 255 / ((float) aW)), currentHSVA));
                canvas.drawLine(i, 0F, i, ((float) aH), aPaint);
            }
            canvas.drawRect(currentXPos[3] - lW, 0F, currentXPos[3] + lW, aH, oppositeColorPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(aW, aH);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSVA[3] = limitValue((x / ((float) aW)), 0F, 1F);
            currentXPos[3] = x;
            invalidateAllViews();
            return true;
        }
    }

    private class SavedColorListView extends BaseView {
        private final int w, h;

        public SavedColorListView(Context context, int w, int h) {
            super(context);
            this.w = w;
            this.h = h;
        }
    }
}