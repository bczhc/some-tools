package pers.zhc.tools.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Selection;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AlertDialog;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ColorUtils;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;


@SuppressWarnings("SameParameterValue")
@SuppressLint("ViewConstructor")
public class HSVAColorPickerRL extends RelativeLayout {
    private float[] currentXPos;
    private final float lW = 1.5F;
    private final float[] hsv = new float[3];
    private int alpha;
    private Paint oppositeColorPaint;
    private final Context context;
    private View[] hsvaViews;
    private ColorView colorView;
    private OnColorPickedInterface onColorPickedInterface = null;
    private final LinearLayout.LayoutParams hsvaViewsParams = new LinearLayout.LayoutParams(0, 0);
    private int width = 0;

    /**
     * @param context context
     * @param alpha   alpha
     * @param hsv     hsv float array
     */
    public HSVAColorPickerRL(Context context, int alpha, float[] hsv) {
        super(context);
        System.arraycopy(hsv, 0, this.hsv, 0, this.hsv.length);
        this.alpha = alpha;
        this.context = context;
        init();
    }

    public HSVAColorPickerRL(Context context) {
        this(context, Color.RED);
    }

    @NotNull
    private static float[] color2hsv(int color) {
        float[] t = new float[3];
        Color.colorToHSV(color, t);
        return t;
    }

    /**
     * @param context      context
     * @param initialColor the initial color
     */
    public HSVAColorPickerRL(Context context, int initialColor) {
        this(context, Color.alpha(initialColor), color2hsv(initialColor));
    }

    /**
     * @param context   context
     * @param hsvaColor {@link ColorUtils.HSVAColor} HSVA color
     */
    public HSVAColorPickerRL(Context context, @NotNull ColorUtils.HSVAColor hsvaColor) {
        this(context, hsvaColor.alpha, hsvaColor.hsv);
    }

    public static int limitValue(int value, int min, int max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static float limitValue(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    private void setCurrentX() {
        currentXPos[0] = hsv[0] * width / (float) 360;
        currentXPos[1] = hsv[1] * width;
        currentXPos[2] = hsv[2] * width;
        currentXPos[3] = alpha * width / 255F;
    }

    private void init() {
        oppositeColorPaint = new Paint();
        oppositeColorPaint.setColor(ColorUtils.invertColor(Color.HSVToColor(alpha, hsv)));

        hsvaViews = new View[]{
                new HView(context),
                new SView(context),
                new VView(context),
                new AView(context)
        };
        currentXPos = new float[hsvaViews.length];

        setCurrentX();

        final View inflate = View.inflate(context, R.layout.hsva_color_picker_view, null);
        final LinearLayout hsvaViewsLL = inflate.findViewById(R.id.hsva_views_ll);
        colorView = inflate.findViewById(R.id.color_view);

        for (View view : hsvaViews) {
            hsvaViewsLL.addView(view);
        }

        colorView.setColor(Color.HSVToColor(alpha, hsv));
        colorView.setOnClickListener(v -> onColorViewClicked());

        this.addView(inflate);
    }

    private void onColorViewClicked() {
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
                Color.colorToHSV(parsedColor, hsv);
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
            LinearLayout saveColorLL = new LinearLayout(this.context);
            HorizontalScrollView saveColorHSV = new HorizontalScrollView(this.context);
            saveColorHSV.addView(saveColorLL);
            final String[] s = {editText.getText().toString()};
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
                EditText nameET = new EditText(this.context);
                nameET.setText(s[0]);
                adb1.setView(nameET);
                adb1.setPositiveButton(R.string.confirm, (dialog1, which1) -> {
                    if (((LinearLayout) this.getChildAt(0)).getChildCount() == 5) {
                        //未保存过颜色时，创建HorizontalScrollView和linearlayout
                        ColorShowRL colorShowRL = new ColorShowRL(this.context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(20, 20, 20, 20);
                        colorShowRL.setLayoutParams(params);
                        colorShowRL.setColor(Color.parseColor(s[0]), nameET.getText().toString());
                        saveColorLL.addView(colorShowRL);
                        ((LinearLayout) this.getChildAt(0)).addView(saveColorHSV);
                    } else {
                        //保存过颜色就直接往里面加colorShowRL
                        ColorShowRL colorShowRL = new ColorShowRL(this.context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(20, 20, 20, 20);
                        colorShowRL.setLayoutParams(params);
                        colorShowRL.setColor(Color.parseColor(s[0]), nameET.getText().toString());
                        ((LinearLayout) ((((HorizontalScrollView) ((LinearLayout) this.getChildAt(0)).getChildAt(5))).getChildAt(0))).addView(colorShowRL);//往saveColorLL里面添加
                    }
                });
                adb1.setNegativeButton(R.string.cancel, (dialog1, which1) -> {
                });
                adb1.setTitle(R.string.color_naming);
                AlertDialog ad1 = adb1.create();
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
    }

    private void invalidateAllViews() {
        final int color = Color.HSVToColor(alpha, hsv);
        if (onColorPickedInterface != null) {
            onColorPickedInterface.onColorPicked(hsv, alpha, color);
        }
        oppositeColorPaint.setColor(ColorUtils.invertColor(color));
        for (View view : hsvaViews) {
            view.invalidate();
        }
        colorView.setColor(color);
    }

    public OnColorPickedInterface getOnColorPickedInterface() {
        return onColorPickedInterface;
    }

    public void setOnColorPickedInterface(OnColorPickedInterface onColorPickedInterface) {
        this.onColorPickedInterface = onColorPickedInterface;
    }

    public int getColor() {
        return Color.HSVToColor(alpha, this.hsv);
    }

    private static class SavedColorListView extends BaseView {

        public SavedColorListView(Context context, int w, int h) {
            super(context);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredWidth = 0;
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        int measureHeight = 0;
        //noinspection SuspiciousNameCombination
        final int expectedHeight = measuredWidth;
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                measureHeight = Math.min(expectedHeight, heightSize);
                break;
            case MeasureSpec.EXACTLY:
                measureHeight = heightSize;
                break;
            case MeasureSpec.UNSPECIFIED:
                measureHeight = expectedHeight;
                break;
        }

        width = measuredWidth;
        int height = measureHeight;

        for (View view : hsvaViews) {
            hsvaViewsParams.width = measuredWidth;
            hsvaViewsParams.height = height / hsvaViews.length;
            view.setLayoutParams(hsvaViewsParams);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private class HView extends BaseView {
        private Paint hPaint;
        private int hW, hH;

        HView(Context context) {
            super(context);
            hInit();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < hW; i++) {
                hPaint.setColor(ColorUtils.HSVAtoColor(alpha, i * 360 / (float) hW, hsv[1], hsv[2]));
                canvas.drawLine(i, 0, i, hH, hPaint);
            }
            canvas.drawRect(currentXPos[0] - lW, 0, currentXPos[0] + lW, hH, oppositeColorPaint);
        }

        private void hInit() {
            hPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            hW = MeasureSpec.getSize(widthMeasureSpec);
            hH = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(hW, hH);
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            hsv[0] = limitValue(x * 360F / ((float) hW), 0, 360);
            currentXPos[0] = limitValue(x, 0, hW);
            invalidateAllViews();
            return true;
        }
    }

    private class SView extends BaseView {
        private int sW;
        private int sH;
        private Paint sPaint;

        SView(Context context) {
            super(context);
            sInit();
        }

        private void sInit() {
            sPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            sW = MeasureSpec.getSize(widthMeasureSpec);
            sH = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(sW, sH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < sW; i++) {
                sPaint.setColor(ColorUtils.HSVAtoColor(alpha, hsv[0], i / (float) sW, hsv[2]));
                canvas.drawLine(i, 0F, i, ((float) sH), sPaint);
            }
            canvas.drawRect(currentXPos[1] - lW, 0F, currentXPos[1] + lW, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            hsv[1] = limitValue(x / ((float) sW), 0, 1F);
            currentXPos[1] = limitValue(x, 0, sW);
            invalidateAllViews();
            return true;
        }
    }

    private class VView extends BaseView {
        private int vW;
        private int vH;
        private Paint vPaint;

        VView(Context context) {
            super(context);
            vInit();
        }

        private void vInit() {
            vPaint = new Paint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            vW = MeasureSpec.getSize(widthMeasureSpec);
            vH = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(vW, vH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < vW; i++) {
                vPaint.setColor(ColorUtils.HSVAtoColor(alpha, hsv[0], hsv[1], i / (float) vW));
                canvas.drawLine(i, 0F, i, ((float) vH), vPaint);
            }
            canvas.drawRect(currentXPos[2] - lW, 0F, currentXPos[2] + lW, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            hsv[2] = limitValue(x / ((float) vW), 0, 1);
            currentXPos[2] = limitValue(x, 0, vW);
            invalidateAllViews();
            return true;
        }
    }

    private class AView extends BaseView {
        private int aW;
        private int aH;
        private Paint aPaint;

        AView(Context context) {
            super(context);
            aInit();
        }

        private void aInit() {
            aPaint = new Paint();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (float i = 0; i < aW; i++) {
                aPaint.setColor(Color.HSVToColor((int) (i * 255 / ((float) aW)), hsv));
                canvas.drawLine(i, 0F, i, ((float) aH), aPaint);
            }
            canvas.drawRect(currentXPos[3] - lW, 0F, currentXPos[3] + lW, aH, oppositeColorPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            aW = MeasureSpec.getSize(widthMeasureSpec);
            aH = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(aW, aH);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            alpha = limitValue(((int) (x * 255)) / aW, 0, 255);
            currentXPos[3] = limitValue(x, 0, aW);
            invalidateAllViews();
            return true;
        }
    }
}
