package pers.zhc.tools.functiondrawing;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.u.math.util.MathFloatFunctionInterface;

import java.util.Objects;

public class FunctionDrawingBoard extends BaseActivity {
    static MathFloatFunctionInterface functionInterface;
    private FunctionDrawingBoardView functionDrawingBoardView;
    private int[] r;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Dialog dialog = new Dialog(this);
        Point point = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(point);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new ViewGroup.LayoutParams(point.x, point.y));
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(lp);
        sv.addView(ll);
        Paint paint = new Paint();
        paint.setTextSize(28);
        paint.setColor(ContextCompat.getColor(this, R.color.hint_color));
        int[] strRes = new int[]{
                R.string.function_drawing_board_a_period_Length,
                R.string.function_drawing_board_a_period_length,
                R.string.function_drawing_x_length,
                R.string.function_drawing_y_length
        };
        int[] initialValue = new int[]{30, 30, 30, 30};
        AppCompatEditText[] editTexts = new AppCompatEditText[4];
        StringBuilder s = new StringBuilder();
        r = new int[4];
        for (int i = 0; i < editTexts.length; i++) {
            int finalI = i;
            editTexts[i] = new AppCompatEditText(this) {
                @Override
                protected void onDraw(Canvas canvas) {
                    s.delete(0, s.length());
                    Editable text = getText();
                    String t = text == null ? "" : text.toString();
                    int length = t.length();
                    for (int j = 0; j < length; j++) {
                        s.append("\u3000");
                    }
                    canvas.drawText(s + getString(strRes[finalI]), 15, 55, paint);
                    super.onDraw(canvas);
                }
            };
            editTexts[i].setLayoutParams(lp);
            editTexts[i].setText(String.valueOf(initialValue[i]));
            editTexts[i].setLayoutParams(new ViewGroup.LayoutParams(point.x, ViewGroup.LayoutParams.WRAP_CONTENT));
            ll.addView(editTexts[i]);
        }
        Button sbm = new Button(this);
        sbm.setOnClickListener(v -> {
            for (int i = 0; i < editTexts.length; i++) {
                try {
                    r[i] = Integer.parseInt(Objects.requireNonNull(editTexts[i].getText()).toString());
                } catch (NumberFormatException | NullPointerException ignored) {
                }
            }
            functionDrawingBoardView = new FunctionDrawingBoardView(this, r);
            setContentView(functionDrawingBoardView);
            dialog.dismiss();
        });
        sbm.setText(R.string.ok);
        ll.addView(sbm);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        dialog.setContentView(sv);
    }

    @Override
    public void onBackPressed() {
        FunctionDrawingBoard.functionInterface = functionDrawingBoardView.getFunction();
        Intent intent = new Intent(this, FunctionDrawing.class);
        intent.putExtra("xLen", r[2]);
        intent.putExtra("yLen", r[3]);
        intent.putExtra("FS_T", r[0]);
        startActivity(intent);
    }
}
