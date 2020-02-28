package pers.zhc.tools.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ScrollView;
import pers.zhc.tools.R;

public class ScrollEditText extends ScrollView implements EditTextInterface {
    private EditText editText;

    public ScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ScrollEditText(Context context) {
        super(context);
    }

    public ScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Context ctx = getContext();
        editText = new EditText(ctx);
        TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.ScrollEditText);
        int gravity = ta.getInt(R.styleable.ScrollEditText_gravity, 1);
        switch (gravity) {
            case 0:
                editText.setGravity(Gravity.TOP);
                break;
            case 1:
                editText.setGravity(Gravity.CENTER);
                break;
            default:
                editText.setGravity(Gravity.NO_GRAVITY);
                break;
        }
        String hint = ta.getString(R.styleable.ScrollEditText_hint);
        editText.setHint(hint);
        editText.setCursorVisible(ta.getBoolean(R.styleable.ScrollEditText_cursorVisible, true));
        editText.setFocusable(ta.getBoolean(R.styleable.ScrollEditText_focusable, true));
        ta.recycle();
        addView(editText);
    }

    @Override
    public Editable getText() {
        return editText.getText();
    }

    @Override
    public void setText(int resId) {
        editText.setText(resId);
    }

    @Override
    public void setText(CharSequence text) {
        editText.setText(text);
    }

    @Override
    public void setHint(CharSequence text) {
        editText.setHint(text);
    }

    @Override
    public void setHint(int hintRes) {
        editText.setHint(hintRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        editText.setWidth(measuredWidth);
        editText.setHeight(measuredHeight);
    }
}
