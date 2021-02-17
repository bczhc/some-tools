package pers.zhc.tools.views;

import android.annotation.SuppressLint;
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
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.appcompat.app.AlertDialog;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ColorUtils;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.DisplayUtil;
import pers.zhc.tools.utils.ToastUtils;
import android.widget.HorizontalScrollView;


@SuppressWarnings("SameParameterValue")
@SuppressLint("ViewConstructor")
public class HSVAColorPickerRelativeLayout extends RelativeLayout {
    private final float[] currentXPos = new float[4];
    private final float lW = 1.5F;
    private final float[] currentHSV = new float[3];
    private int alpha;
    private Paint oppositeColorPaint;
    private int width;
    private int height;
    private Context context;
    private View[] hsvaViews;
    private View colorPreviewView;
    private OnColorPickedInterface onColorPickedInterface = null;

    /**
     * @param context      context
     * @param initialColor the initial color
     * @param width        view width
     * @param height       view height
     */
    public HSVAColorPickerRelativeLayout(Context context, int initialColor, int width, int height) {
        super(context);
        Color.colorToHSV(initialColor, currentHSV);
        alpha = Color.alpha(initialColor);
        init(context, width, height);
    }

    /**
     * @param context context
     * @param alpha   alpha
     * @param hsv     hsv float array
     * @param width   view's width
     * @param height  view's height
     */
    public HSVAColorPickerRelativeLayout(Context context, int alpha, float[] hsv, int width, int height) {
        super(context);
        System.arraycopy(hsv, 0, currentHSV, 0, currentHSV.length);
        this.alpha = alpha;
        init(context, width, height);
    }

    /**
     * @param context   context
     * @param hsvaColor {@link ColorUtils.HSVAColor} HSVA color
     * @param width     view's width
     * @param height    view's height
     */
    public HSVAColorPickerRelativeLayout(Context context, ColorUtils.HSVAColor hsvaColor, int width, int height) {
        super(context);
        System.arraycopy(hsvaColor.hsv, 0, this.currentHSV, 0, currentHSV.length);
        this.alpha = hsvaColor.alpha;
        init(context, width, height);
    }

    public static int limitValue(int value, int min, int max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static float limitValue(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    private void setCurrentX() {
        currentXPos[0] = currentHSV[0] * width / (float) 360;
        currentXPos[1] = currentHSV[1] * width;
        currentXPos[2] = currentHSV[2] * width;
        currentXPos[3] = alpha * width / 255F;
    }

    private void init(Context context, int width, int height) {
        this.context = context;
        this.width = width;
        this.height = height;
        int perViewHeight = this.height / 4;
        oppositeColorPaint = new Paint();
        oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(alpha, currentHSV)));
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
        tv.setText(R.string.hsva_color_picker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setAutoSizeTextTypeUniformWithConfiguration(1, 200, 1, TypedValue.COMPLEX_UNIT_SP);
        } else {
            float r = ((float) this.height) / 12;
            tv.setTextSize(DisplayUtil.px2sp(this.context, r));
        }
        tv.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setBackgroundColor(Color.WHITE);
        tv.setId(R.id.tv);
        class ColorParseException extends Exception {
        }
        barRL.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this.context);
            EditText editText = new EditText(this.context);
            int color = this.getColor();
            String hexString = ColorUtils.getHexString(color, true);
            editText.setText(hexString);
            adb.setView(editText);
            adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
               String s = editText.getText().toString();
                try {
                    if (s.charAt(0) == '#') s = s.substring(1);
                    if (s.length() < 6 || s.length() > 8)
                        throw new Exception("Illegal color hex string.");
                    if (s.length() == 6) s = "#FF" + s;
                    else if (s.length() == 7) s = "#0" + s;
                    else {
                        s = "#" + s;
                    }
                    final int parsedColor = Color.parseColor(s);
                    Color.colorToHSV(parsedColor, currentHSV);
                    alpha = Color.alpha(parsedColor);
                    setCurrentX();
                    invalidateAllViews();
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
//保存颜色
    LinearLayout saveColorLL=new LinearLayout(this.context);
    HorizontalScrollView saveColorHSV=new HorizontalScrollView(this.context);
    saveColorHSV.addView(saveColorLL);
   final String[] s ={editText.getText().toString()};
    try {
        if (s[0].charAt(0) == '#') s[0] = s[0].substring(1);
        if (s[0].length() < 6 || s[0].length() > 8)
            throw new Exception("Illegal color hex string.");
        if (s[0].length() == 6) s[0] = "#FF" + s[0];
        else if (s[0].length() == 7) s[0] = "#0" + s[0];
        else {
            s[0] = "#" + s[0];
        }
        AlertDialog.Builder adb1 = new AlertDialog.Builder(this.context);
        EditText nameET=new EditText(this.context);
        nameET.setText(s[0]);
        adb1.setView(nameET);
        adb1.setPositiveButton(R.string.confirm, (dialog1, which1) -> {
        if(((LinearLayout)this.getChildAt(0)).getChildCount() == 5)
        {
            //未保存过颜色时，创建HorizontalScrollView和linearlayout
            ColorShowRL colorShowRL = new ColorShowRL(this.context) ;
            LinearLayout.LayoutParams params= new LinearLayout.LayoutParams (LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(20,20,20,20);
            colorShowRL.setLayoutParams(params);
            colorShowRL.setColor(Color.parseColor(s[0]),nameET.getText().toString());
            saveColorLL.addView(colorShowRL);
            ((LinearLayout)this.getChildAt(0)).addView(saveColorHSV);
        }
        else
        {
            //保存过颜色就直接往里面加colorShowRL
            ColorShowRL colorShowRL = new ColorShowRL(this.context) ;
            LinearLayout.LayoutParams params= new LinearLayout.LayoutParams (LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(20,20,20,20);
            colorShowRL.setLayoutParams(params);
            colorShowRL.setColor(Color.parseColor(s[0]),nameET.getText().toString());
            ((LinearLayout)((((HorizontalScrollView)((LinearLayout)this.getChildAt(0)).getChildAt(5))).getChildAt(0))).addView(colorShowRL);//往saveColorLL里面添加
            }
      });
        adb1.setNegativeButton(R.string.cancel, (dialog1, which1) -> {
        });
        adb1.setTitle(R.string.color_naming);
       AlertDialog ad1= adb1.create();
        DialogUtil.setDialogAttr(ad1, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
        DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(editText, ad1);
        Selection.selectAll(nameET.getText());
        ad1.show();
    } catch (Exception e) {
        e.printStackTrace();
        ToastUtils.show(this.context, R.string.please_type_correct_value);
    }

            });
            DialogUtil.setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(editText, ad);
            Selection.selectAll(editText.getText());
            ad.show();
        });
        barRL.addView(tv);
        colorPreviewView = new View(this.context) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawColor(Color.HSVToColor(alpha, currentHSV));
            }
        };
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.tv);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.tv);
        colorPreviewView.setLayoutParams(layoutParams);
        barRL.addView(colorPreviewView);
        ll.addView(barRL);
        LinearLayout[] linearLayouts = new LinearLayout[hsvaViews.length];
        for (int i = 0; i < linearLayouts.length; i++) {
            linearLayouts[i] = new LinearLayout(this.context);
            linearLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(this.width, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            linearLayouts[i].addView(hsvaViews[i]);
            ll.addView(linearLayouts[i]);
        }
        this.addView(ll);
    }

    private void invalidateAllViews() {
        if (onColorPickedInterface != null)
            onColorPickedInterface.onColorPicked(currentHSV[0], currentHSV[1], currentHSV[2], alpha);
        else onColorPicked(currentHSV[0], currentHSV[1], currentHSV[2], alpha);
        oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(alpha, currentHSV)));
        for (View view : hsvaViews) {
            view.invalidate();
        }
        colorPreviewView.invalidate();
    }

    public OnColorPickedInterface getOnColorPickedInterface() {
        return onColorPickedInterface;
    }

    public void setOnColorPickedInterface(OnColorPickedInterface onColorPickedInterface) {
        this.onColorPickedInterface = onColorPickedInterface;
    }

    private int getColor() {
        return Color.HSVToColor(alpha, this.currentHSV);
    }

    /**
     * Callback by overriding
     *
     * @param h     h
     * @param s     s
     * @param v     v
     * @param alpha a
     */
    public void onColorPicked(@FloatRange(from = 0, to = 1) float h,
                              @FloatRange(from = 0, to = 1) float s,
                              @FloatRange(from = 0, to = 1) float v,
                              @IntRange(from = 0, to = 255) int alpha) {

    }

    private static class SavedColorListView extends BaseView {

        public SavedColorListView(Context context, int w, int h) {
            super(context);
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
            for (float i = 0; i < hW; i++) {
                hPaint.setColor(ColorUtils.HSVAtoColor(alpha, i * 360 / (float) hW, currentHSV[1], currentHSV[2]));
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
            currentHSV[0] = limitValue(x * 360F / ((float) hW), 0, 360);
            currentXPos[0] = limitValue(x, 0, hW);
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
                sPaint.setColor(ColorUtils.HSVAtoColor(alpha, currentHSV[0], i / (float) sW, currentHSV[2]));
                canvas.drawLine(i, 0F, i, ((float) height), sPaint);
            }
            canvas.drawRect(currentXPos[1] - lW, 0F, currentXPos[1] + lW, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSV[1] = limitValue(x / ((float) sW), 0, 1F);
            currentXPos[1] = limitValue(x, 0, sW);
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
                vPaint.setColor(ColorUtils.HSVAtoColor(alpha, currentHSV[0], currentHSV[1], i / (float) vW));
                canvas.drawLine(i, 0F, i, ((float) height), vPaint);
            }
            canvas.drawRect(currentXPos[2] - lW, 0F, currentXPos[2] + lW, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float x = event.getX();
            currentHSV[2] = limitValue(x / ((float) vW), 0, 1);
            currentXPos[2] = limitValue(x, 0, vW);
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
                aPaint.setColor(Color.HSVToColor((int) (i * 255 / ((float) aW)), currentHSV));
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
            alpha = limitValue(((int) (x * 255)) / aW, 0, 255);
            currentXPos[3] = limitValue(x, 0, aW);
            invalidateAllViews();
            return true;
        }
    }
}
