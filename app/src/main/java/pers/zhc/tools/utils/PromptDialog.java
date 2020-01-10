package pers.zhc.tools.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import pers.zhc.tools.R;

import static pers.zhc.tools.floatingboard.FloatingBoardMainActivity.getSelectedET_currentMills;

@SuppressWarnings("unused")
public class PromptDialog extends Dialog {
    private Context ctx;
    private EditText et;

    public PromptDialog(@NonNull Context context, @StringRes int titleRes
            , @StringRes int positiveBtnName, DialogInterface positiveAction, @StringRes int negativeBtnName, DialogInterface negativeAction) {
        super(context);
        this.ctx = context;
        init(titleRes, positiveBtnName, positiveAction, negativeBtnName, negativeAction);
    }

    private void init(int titleRes, int positiveBtnName, DialogInterface positiveAction, int negativeBtnName, DialogInterface negativeAction) {
        View inflate = View.inflate(ctx, R.layout.prompt_dialog_rl, null);
        TextView title = inflate.findViewById(R.id.title);
        et = inflate.findViewById(R.id.et);
        title.setText(titleRes);
        Button positiveBtn = inflate.findViewById(R.id.positive);
        Button negativeBtn = inflate.findViewById(R.id.negative);
        positiveBtn.setText(positiveBtnName);
        positiveBtn.setOnClickListener(v -> {
            positiveAction.onClick(this, et);
            this.dismiss();
        });
        negativeBtn.setText(negativeBtnName);
        negativeBtn.setOnClickListener(v -> {
            negativeAction.onClick(this, et);
            this.dismiss();
        });
        this.setContentView(inflate, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
    }

    public EditText getEditText() {
        return this.et;
    }

    @Override
    public void show() {
        getSelectedET_currentMills(ctx, et);
        this.setOnShowListener(dialog -> new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ((Activity) ctx).runOnUiThread(() -> {
                et.requestFocus();
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
            });
        }).start());
        super.show();
    }

    public interface DialogInterface {
        void onClick(Dialog dialog, EditText et);
    }
}