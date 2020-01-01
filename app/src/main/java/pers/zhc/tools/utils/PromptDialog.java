package pers.zhc.tools.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
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
        /*RelativeLayout rl = new RelativeLayout(ctx);
        RelativeLayout.LayoutParams positiveBtnLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams negativeBtnLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Button positiveBtn = new Button(ctx);
        Button negativeBtn = new Button(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            positiveBtnLP.addRule(RelativeLayout.ALIGN_PARENT_END);
        } else positiveBtnLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        } else negativeBtnLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        positiveBtn.setLayoutParams(positiveBtnLP);
        negativeBtn.setLayoutParams(negativeBtnLP);*/
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
    }

    public EditText getEditText() {
        return this.et;
    }

    @Override
    public void show() {
        Handler handler = new Handler();
        this.setOnShowListener(dialog -> new Thread(() -> handler.postDelayed(() -> ((Activity) ctx).runOnUiThread(() -> {
            et.requestFocus();
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
        }), 300)).start());
        getSelectedET_currentMills(ctx, et);
        super.show();
    }

    public interface DialogInterface {
        void onClick(Dialog dialog, EditText et);
    }
}