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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.*;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


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
    private final int width = 0;
    private RecyclerView savedColorRV;
    private ArrayList<SavedColor> savedColors = new ArrayList<>();
    private SavedColorAdapter savedColorAdapter;

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

    private void updateCurrentX() {
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
        for (View v : hsvaViews) {
            v.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, DisplayUtil.dip2px(context, 63 /* 1cm */)));
        }
        currentXPos = new float[hsvaViews.length];

        updateCurrentX();

        final View inflate = View.inflate(context, R.layout.hsva_color_picker_view, null);
        final LinearLayout hsvaViewsLL = inflate.findViewById(R.id.hsva_views_ll);
        colorView = inflate.findViewById(R.id.color_view);
        savedColorRV = inflate.findViewById(R.id.recycler_view);

        for (View view : hsvaViews) {
            hsvaViewsLL.addView(view);
        }

        colorView.setColor(Color.HSVToColor(alpha, hsv));
        colorView.setOnClickListener(v -> onColorViewClicked());

        this.addView(inflate);

        savedColorAdapter = new SavedColorAdapter(context, savedColors);
        savedColorRV.setAdapter(savedColorAdapter);
        savedColorRV.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        savedColorAdapter.setOnColorViewClickedListener(self -> {
            setColor(self.getColor());
        });
        savedColorAdapter.setOnItemLongClickListener((view, position) -> {
            final PopupMenu popupMenu = PopupMenuUtil.createPopupMenu(context, view, R.menu.color_picker_saved_color_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();

                if (itemId == R.id.delete) {
                    savedColors.remove(position);
                    savedColorAdapter.notifyItemRemoved(position);
                }
                return true;
            });
            popupMenu.show();
        });
    }

    private void onColorViewClicked() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this.context);
        EditText editText = new EditText(this.context);
        int color = this.getColor();
        String hexString = ColorUtils.getHexString(color, true);
        editText.setText(hexString);
        adb.setView(editText);
        adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
            String input = editText.getText().toString();
            try {
                final int parsed = Color.parseColor(input);
                Color.colorToHSV(parsed, hsv);
                alpha = Color.alpha(parsed);
                updateCurrentX();
                invalidateAllViews();
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.showError(context, R.string.please_enter_correct_value_toast, e);
            }
        });
        adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
        });
        adb.setTitle(R.string.please_enter_color_hex);
        AlertDialog ad = adb.create();
        ad.setButton(AlertDialog.BUTTON_NEUTRAL, this.context.getString(R.string.save_color), (dialog, which) -> {

            // 保存颜色
            final String input = editText.getText().toString();
            int parsed;
            try {
                parsed = Color.parseColor(input);
            } catch (Exception e) {
                ToastUtils.showError(context, R.string.please_enter_correct_value_toast, e);
                return;
            }

            final EditText namingET = new EditText(context);
            final AlertDialog namingDialog = DialogUtils.Companion.createPromptDialog(
                    context,
                    R.string.color_naming,
                    (dialogInterface, editText12) -> {

                        savedColors.add(new SavedColor(parsed, editText12.getText().toString()));
                        savedColorAdapter.notifyItemInserted(savedColors.size() - 1);

                        return Unit.INSTANCE;
                    },
                    (dialogInterface, editText1) -> Unit.INSTANCE,
                    namingET
            );
            namingET.setText(ColorUtils.getHexString(parsed, true));
            DialogUtil.setDialogAttr(namingDialog, null);
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(namingET, namingDialog);
            Selection.selectAll(namingET.getText());
            namingDialog.show();

        });
        DialogUtil.setDialogAttr(ad, false, WRAP_CONTENT, WRAP_CONTENT, null);
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

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        alpha = Color.alpha(color);
        updateCurrentX();
        invalidateAllViews();
    }

    public ArrayList<SavedColor> getSavedColors() {
        ArrayList<SavedColor> list = new ArrayList<>();

        final int childCount = savedColorRV.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final ColorShowRL colorShowRL = (ColorShowRL) savedColorRV.getChildAt(i);
            list.add(new SavedColor(colorShowRL.getColor(), colorShowRL.getName()));
        }
        return list;
    }

    public void setSavedColor(ArrayList<SavedColor> savedColors) {
        this.savedColors.clear();
        for (SavedColor savedColor : savedColors) {
            this.savedColors.add(savedColor);
        }
        savedColorAdapter.notifyDataSetChanged();
    }

    private static class SavedColorListView extends BaseView {

        public SavedColorListView(Context context, int w, int h) {
            super(context);
        }
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

    public static class SavedColor {
        public final int color;
        public final String name;

        public SavedColor(int color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    private static class SavedColorAdapter extends RecyclerView.Adapter<SavedColorAdapter.MyViewHolder> {
        private final Context context;
        private final ArrayList<SavedColor> data;
        @Nullable
        private ColorShowRL.OnColorViewClickedListener onColorViewClickedListener = null;
        @Nullable
        private OnItemLongClickListener onItemLongClickListener = null;

        private static class MyViewHolder extends RecyclerView.ViewHolder {
            public MyViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }

            private ColorShowRL getView() {
                return ((ColorShowRL) itemView);
            }
        }

        public void setOnColorViewClickedListener(ColorShowRL.@Nullable OnColorViewClickedListener onColorViewClickedListener) {
            this.onColorViewClickedListener = onColorViewClickedListener;
        }

        public void setOnItemLongClickListener(@Nullable OnItemLongClickListener onItemLongClickListener) {
            this.onItemLongClickListener = onItemLongClickListener;
        }

        public SavedColorAdapter(Context context, ArrayList<SavedColor> data) {
            this.context = context;
            this.data = data;
        }

        @NonNull
        @NotNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            return new MyViewHolder(new ColorShowRL(context));
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull HSVAColorPickerRL.SavedColorAdapter.MyViewHolder holder, int position) {
            final ColorShowRL colorShowRL = holder.getView();
            final SavedColor savedColor = data.get(position);
            colorShowRL.setColor(savedColor.color, savedColor.name);

            colorShowRL.setOnColorViewClickedListener(self -> {
                if (onColorViewClickedListener != null) {
                    onColorViewClickedListener.onClick(self);
                }
            });
            colorShowRL.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onLongClick(v, holder.getLayoutPosition());
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private interface OnItemLongClickListener {
            void onLongClick(View view, int position);
        }
    }
}
