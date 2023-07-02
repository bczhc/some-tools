package pers.zhc.tools.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Selection;
import android.util.AttributeSet;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import kotlin.Unit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


@SuppressWarnings("SameParameterValue")
@SuppressLint("ViewConstructor")
public class HSVAColorPickerRL extends RelativeLayout {
    private float[] currentXPos;
    private float lineExtrusion = -1;
    private final float[] hsv = new float[3];
    private int alpha;
    private Paint oppositeColorPaint;
    private final Context context;
    private View[] hsvaViews;
    private ColorView colorView;
    private OnColorPickedInterface onColorPickedInterface = null;
    private int width = -1;
    private RecyclerView savedColorRV;
    private final List<SavedColor> savedColors = new ArrayList<>();
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

    public HSVAColorPickerRL(Context context, AttributeSet attrs) {
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

        lineExtrusion = DisplayUtil.mm2px(context, 0.15F);

        hsvaViews = new View[]{
                new HView(context),
                new SView(context),
                new VView(context),
                new AView(context)
        };
        for (View v : hsvaViews) {
            v.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ((int) DisplayUtil.cm2px(context, 1.3F))));
        }
        currentXPos = new float[hsvaViews.length];

        final View inflate = View.inflate(context, R.layout.hsva_color_picker_view, null);
        final LinearLayout hsvaViewsLL = inflate.findViewById(R.id.hsva_views_ll);
        colorView = inflate.findViewById(R.id.color_view);
        savedColorRV = inflate.findViewById(R.id.recycler_view);

        for (View view : hsvaViews) {
            hsvaViewsLL.addView(view);
        }

        colorView.setColor(Color.HSVToColor(alpha, hsv));
        colorView.setOnClickListener(v -> showSavingColorDialog());
        colorView.setOnLongClickListener(v -> {
            saveColor(getColor(), ColorUtils.getHexString(getColor(), true));
            return true;
        });

        this.addView(inflate);

        savedColorAdapter = new SavedColorAdapter(context, savedColors);
        savedColorRV.setAdapter(savedColorAdapter);
        savedColorRV.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        savedColorAdapter.setOnColorViewClickedListener(self -> {
            final SavedColor savedColor = self.getColor();
            setColor(savedColor.hsv, savedColor.alpha);
            int color = Color.HSVToColor(savedColor.alpha, savedColor.hsv);
            onColorPickedInterface.onColorPicked(savedColor.hsv, savedColor.alpha, color, true);
        });
        savedColorAdapter.setOnItemLongClickListener((view, position) -> {
            final PopupMenu popupMenu = PopupMenuUtil.createPopupMenu(context, view, R.menu.color_picker_saved_color_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();

                if (itemId == R.id.delete) {
                    savedColors.remove(position);
                    savedColorAdapter.notifyItemRemoved(position);
                } else if (itemId == R.id.edit) {
                    showSavedColorEditingDialog(position);
                }
                return true;
            });
            popupMenu.show();
        });

        savedColorRV.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull @NotNull Rect outRect, @NonNull @NotNull View view,
                                       @NonNull @NotNull RecyclerView parent, @NonNull @NotNull RecyclerView.State state) {
                outRect.right = DisplayUtil.dip2px(context, 7F);
            }
        });
    }

    private void showSavedColorEditingDialog(int position) {
        SavedColor savedColor = savedColors.get(position);

        EditText editText = new EditText(context);
        editText.setText(savedColor.name);

        try {
            int color = ColorUtils.parseColorHex(savedColor.name);
            if (savedColor.getColorInt() == color) {
                // the saved color name is the hex code of itself
                // fill with the new color name
                editText.setText(ColorUtils.getHexString(Color.HSVToColor(alpha, hsv), true));
            }
        } catch (IllegalArgumentException ignored) {
        }


        AlertDialog dialog = DialogUtils.Companion.createPromptDialog(context, R.string.color_naming, (dialogInterface, et) -> {

            savedColors.set(position, new SavedColor(savedColor.hsv, savedColor.alpha, editText.getText().toString()));
            savedColorAdapter.notifyItemChanged(position);

            return Unit.INSTANCE;
        }, (dialogInterface, et) -> Unit.INSTANCE, editText);
        dialog.setButton(
                DialogInterface.BUTTON_NEUTRAL,
                context.getString(R.string.color_picker_update_color), (d, which) -> {
                    savedColors.set(position, new SavedColor(hsv, alpha, editText.getText().toString()));
                    savedColorAdapter.notifyItemChanged(position);
                }
        );
        DialogUtil.setDialogAttr(dialog, null);
        dialog.show();
    }

    private void saveColor(int color, String name) {
        float[] hsv = new float[3];
        int alpha = Color.alpha(color);
        Color.colorToHSV(color, hsv);
        savedColors.add(new SavedColor(hsv, alpha, name));
        savedColorAdapter.notifyItemInserted(savedColors.size() - 1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        updateCurrentX();
    }

    private void showSavingColorDialog() {
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
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
                invalidateAllViews(true);
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
            int colorToSave;
            try {
                colorToSave = ColorUtils.parseColorHex(input);
            } catch (Exception ignored) {
                ToastUtils.show(context, R.string.please_enter_correct_value_toast);
                return;
            }

            final EditText namingET = new EditText(context);
            final AlertDialog namingDialog = DialogUtils.Companion.createPromptDialog(
                    context,
                    R.string.color_naming,
                    (dialogInterface, editText12) -> {

                        saveColor(colorToSave, editText12.getText().toString());

                        return Unit.INSTANCE;
                    },
                    (dialogInterface, editText1) -> Unit.INSTANCE,
                    namingET
            );
            namingET.setText(ColorUtils.getHexString(colorToSave, true));
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

    private void invalidateAllViews(boolean fromUser) {
        final int color = Color.HSVToColor(alpha, hsv);
        if (onColorPickedInterface != null) {
            onColorPickedInterface.onColorPicked(hsv, alpha, color, fromUser);
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
        invalidateAllViews(false);
    }

    public void setColor(float[] hsv, int alpha) {
        System.arraycopy(hsv, 0, this.hsv, 0, 3);
        this.alpha = alpha;
        updateCurrentX();
        invalidateAllViews(false);
    }

    public List<SavedColor> getSavedColors() {
        return savedColorAdapter.getData();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSavedColor(List<SavedColor> savedColors) {
        this.savedColors.clear();
        this.savedColors.addAll(savedColors);
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
            canvas.drawRect(currentXPos[0] - lineExtrusion, 0, currentXPos[0] + lineExtrusion, hH, oppositeColorPaint);
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
            invalidateAllViews(true);
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
            canvas.drawRect(currentXPos[1] - lineExtrusion, 0F, currentXPos[1] + lineExtrusion, sH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            hsv[1] = limitValue(x / ((float) sW), 0, 1F);
            currentXPos[1] = limitValue(x, 0, sW);
            invalidateAllViews(true);
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
            canvas.drawRect(currentXPos[2] - lineExtrusion, 0F, currentXPos[2] + lineExtrusion, vH, oppositeColorPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(@NotNull MotionEvent event) {
            final float x = event.getX();
            hsv[2] = limitValue(x / ((float) vW), 0, 1);
            currentXPos[2] = limitValue(x, 0, vW);
            invalidateAllViews(true);
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
            canvas.drawRect(currentXPos[3] - lineExtrusion, 0F, currentXPos[3] + lineExtrusion, aH, oppositeColorPaint);
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
            invalidateAllViews(true);
            return true;
        }
    }

    public static class SavedColor {
        public final float[] hsv;
        public final int alpha;
        public final String name;

        public SavedColor(float[] hsv, int alpha, String name) {
            float[] cloned = new float[3];
            System.arraycopy(hsv, 0, cloned, 0, 3);
            this.hsv = cloned;
            this.alpha = alpha;
            this.name = name;
        }

        public int getColorInt() {
            return Color.HSVToColor(alpha, hsv);
        }

        @Contract(value = "null -> false", pure = true)
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SavedColor that = (SavedColor) o;

            if (alpha != that.alpha) return false;
            if (!Arrays.equals(hsv, that.hsv)) return false;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(hsv);
            result = 31 * result + alpha;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    private static class SavedColorAdapter extends RecyclerView.Adapter<SavedColorAdapter.MyViewHolder> {
        private final Context context;
        private final List<SavedColor> data;
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

        public SavedColorAdapter(Context context, List<SavedColor> data) {
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
            colorShowRL.setColor(savedColor);
            colorShowRL.setName(savedColor.name);

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

        public List<SavedColor> getData() {
            return data;
        }
    }
}
