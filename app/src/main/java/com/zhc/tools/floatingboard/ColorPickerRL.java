package com.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.zhc.tools.R;

@SuppressLint("ViewConstructor")
abstract class ColorPickerRL extends RelativeLayout {

    ColorPickerRL(Context context, int dialogWidth, int dialogHeight, int initializeColor) {
        super(context);
        RLInitialize(context, dialogWidth, dialogHeight, initializeColor);
    }

    private void RLInitialize(Context context, int dialogWidth, int dialogHeight, int initializeColor) {
        ColorPickerLL colorPickerLL = new ColorPickerLL(context, dialogWidth, dialogHeight, initializeColor) {
            @Override
            void onDoneBtnPressed(int pickedColor) {
//                super.onDoneBtnPressed(pickedColor);
                ColorPickerRL.this.onDoneBtnPressed(pickedColor);
            }

            @Override
            void onPickedAction(int color) {
//                super.onPickedAction(color);
                ColorPickerRL.this.onPickedAction(color);
            }
        };
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.addView(colorPickerLL);
    }

    abstract void onPickedAction(int color);

    abstract void onDoneBtnPressed(int color);

    @SuppressLint("ViewConstructor")
    abstract class ColorPickerLL extends LinearLayout {
        private Button doneBtn;
        private int gradientWidth;
        private int dialogHeight;
        private int dialogWidth;
        private int pickedColor;
        private int alpha = 255;
        private final int initializeColor;
        private SaturationLG slg;
        private BrightLG brightLG;

        ColorPickerLL(Context context, int dialogWidth, int dialogHeight, int initializeColor) {
            super(context);
            this.dialogHeight = dialogHeight;
            this.dialogWidth = dialogWidth;
            this.gradientWidth = dialogWidth / 6;
            this.initializeColor = initializeColor;
            init(context);
        }

        private void init(Context context) {
            pickedColor = initializeColor;
            doneBtn = new Button(context);
            doneBtn.setOnClickListener(v -> onDoneBtnPressed(pickedColor));
            doneBtn.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LayoutParams dialog_ll_lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            //        dialog_ll_lp.setMargins(10, 10, 10, 10);
            alpha = initializeColor >>> 24;
            setLayoutParams(dialog_ll_lp);
            setOrientation(LinearLayout.HORIZONTAL);
            int[] colors = {0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
                    0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
            LinearGradient lg = new LinearGradient(0F, 0F, ((float) (gradientWidth)), ((float) dialogHeight), colors, null, LinearGradient.TileMode.MIRROR);
            Paint paint = new Paint();
            paint.setShader(lg);
            Canvas canvas = new Canvas();
            canvas.drawRect(0F, 0F, ((float) (gradientWidth)), ((float) dialogHeight), paint);
            brightLG = new BrightLG(context, dialogWidth, dialogHeight, gradientWidth) {
                @Override
                void onPickedAction(int color) {
                    //                super.onPickedAction(color);
                    pickedColor = getColorWithAlpha(color, alpha);
                    if (slg != null) slg.setSrcColor(color);
                    setBtnColor();
                }
            };
            ColorPickerLeftGradient pickerView = new ColorPickerLeftGradient(context, gradientWidth, dialogHeight) {
                @Override
                void onPickedAction(int color) {
                    pickedColor = getColorWithAlpha(color, alpha);
                    brightLG.setIntermediateColor(pickedColor);
                    if (slg != null) slg.setSrcColor(pickedColor);
                    setBtnColor();
                }
            };
            brightLG.setIntermediateColor(pickedColor);
            setBtnColor();
            addView(pickerView);
            LinearLayout dialog_ll_right = new LinearLayout(context);
            dialog_ll_right.setLayoutParams(dialog_ll_lp);
            dialog_ll_right.setOrientation(LinearLayout.VERTICAL);
            dialog_ll_right.addView(brightLG);
            slg = new SaturationLG(context, dialogWidth - gradientWidth, dialogHeight / 10) {
                @Override
                void onPickedAction(int color) {
                    //                super.onPickedAction(color);
                    pickedColor = getColorWithAlpha(color, alpha);
                    brightLG.setIntermediateColor(color);
                    setBtnColor();
                }
            };
            slg.setSrcColor(pickedColor);
            dialog_ll_right.addView(slg);
            TextView alpha_tv = new TextView(context);
            alpha_tv.setText(R.string.alpha);
            alpha_tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog_ll_right.addView(alpha_tv);
            SeekBar sb = new SeekBar(context);
            sb.setMax(255);
            sb.setProgress(alpha);
            doneBtn.setText(R.string.done);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                doneBtn.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            } else doneBtn.setTextSize(25F);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    alpha = progress;
                    int colorWithAlpha = getColorWithAlpha(pickedColor, alpha);
                    if (slg != null) slg.setSrcColor(colorWithAlpha);
                    brightLG.setIntermediateColor(colorWithAlpha);
                    setBtnColor();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            sb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog_ll_right.addView(sb);
            dialog_ll_right.addView(doneBtn);
            addView(dialog_ll_right);
        }

        void onDoneBtnPressed(int pickedColor) {
        }

        private void setBtnColor() {
            pickedColor = getColorWithAlpha(pickedColor, this.alpha);
            doneBtn.setBackgroundColor(pickedColor);
            onPickedAction(pickedColor);
        }

        private int getColorWithAlpha(int color, int alpha) {
            RGB rgb = GradientUtil.parseRGB(color);
            color = Color.argb(alpha, rgb.r, rgb.g, rgb.b);
            return color;
        }

        abstract void onPickedAction(int color);

        abstract class BrightLG extends View {
            private int[] colors1;
            private LinearGradient lg;
            private Paint mPaint = new Paint();
            private int dialogHeight;
            int rightWidth;

            BrightLG(Context context, int dialogWidth, int dialogHeight, int gradientWidth) {
                super(context);
                this.dialogHeight = dialogHeight;
                rightWidth = dialogWidth - gradientWidth;
                colors1 = new int[]{0xFF000000, 0xFFFFFFFF};
                lg = new LinearGradient(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), colors1, null, Shader.TileMode.CLAMP);
                invalidate();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                //                                    super.onDraw(canvas);
                mPaint.setShader(lg);
                canvas.drawRect(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), mPaint);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                //                                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(rightWidth, dialogHeight / 10);
            }

            void setIntermediateColor(int colorInt) {
                this.colors1 = new int[]{0xFF000000, colorInt, 0xFFFFFFFF};
                this.lg = new LinearGradient(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), colors1, null, Shader.TileMode.CLAMP);
                invalidate();
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                try {
                    onPickedAction(new GradientUtil(colors1, 0F, rightWidth).getColor(event.getX()));
                } catch (Exception ignored) {
                    //                e.printStackTrace();
                }
                return true;
            }

            abstract void onPickedAction(int color);
        }

        abstract class SaturationLG extends View {
            private int width, height;
            private int[] colors;
            private Paint mPaint;
            private LinearGradient lg;

            SaturationLG(Context context, int width, int height) {
                super(context);
                this.width = width;
                this.height = height;
                saturationInit();
            }

            private void saturationInit() {
                colors = new int[]{Color.WHITE, 0xFF808080};
                lg = new LinearGradient(0F, 0F, width, height, colors, null, LinearGradient.TileMode.CLAMP);
                mPaint = new Paint();
                mPaint.setShader(lg);
                invalidate();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                //            super.onDraw(canvas);
                canvas.drawRect(0F, 0F, width, height, mPaint);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                //            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(width, height);
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                //            float sat = event.getX() / width;
                //            RGB rgb = GradientUtil.parseRGB(colors[0]);
                /*float[] hsv = new float[3];
                Color.RGBToHSV(rgb.r, rgb.g, rgb.b, hsv);
                hsv[1] = sat;
                System.out.println("sat = " + sat);
                int color = Color.HSVToColor(hsv);*/
                try {
                    onPickedAction(new GradientUtil(colors, 0F, width).getColor(event.getX()));
                } catch (Exception ignored) {
                }
                return true;
            }

            abstract void onPickedAction(int color);

            void setSrcColor(int color) {
                colors[0] = color;
                lg = new LinearGradient(0F, 0F, width, height, colors, null, LinearGradient.TileMode.CLAMP);
                mPaint.setShader(lg);
                invalidate();
            }
        }
    }

    abstract static class ColorPickerLeftGradient extends View {
        private Paint mPaint = null;
        private int mHeight;
        private int mWidth;
        private GradientUtil colorUtil;

        ColorPickerLeftGradient(Context context, int width, int height) {
            super(context);
            setMinimumWidth(width);
            setMinimumHeight(height);
            mWidth = width;
            mHeight = height;
            init();
        }

        /*public ColorPickerLeftGradient(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }*/

        private void init() {
            mPaint = new Paint();
            int[] colors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
            LinearGradient lg = new LinearGradient(0F, 0F, mWidth, mHeight, colors, null, LinearGradient.TileMode.MIRROR);
            mPaint.setShader(lg);
            colorUtil = new GradientUtil(colors, 0F, mHeight);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //        super.onDraw(canvas);
            canvas.drawRect(0F, 0F, mWidth, mHeight, mPaint);
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                onPickedAction(colorUtil.getColor(event.getY()));
            } catch (Exception ignored) {
            }
            return true;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            //        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(mWidth, mHeight);
        }

        abstract void onPickedAction(int color);
    }
}