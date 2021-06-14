package pers.zhc.tools.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.EditTextInterface;

public class ScrollEditText extends ScrollView implements EditTextInterface {
    private EditText editText;

    public ScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ScrollEditText(Context context) {
        this(context, null);
    }

    public ScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Context ctx = getContext();
        editText = new EditText(ctx);
        editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (attrs != null) {
            setAttrs(attrs);
        }
        addView(editText);
    }

    private void setAttrs(@NotNull AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ScrollEditText);
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
        String text = ta.getString(R.styleable.ScrollEditText_text);
        if (hint != null) {
            editText.setHint(hint);
        }
        if (text != null) {
            editText.setText(text);
        }
        editText.setCursorVisible(ta.getBoolean(R.styleable.ScrollEditText_cursorVisible, true));
        editText.setFocusable(ta.getBoolean(R.styleable.ScrollEditText_focusable, true));
        ta.recycle();
    }

    @Override
    public boolean callOnClick() {
        editText.requestFocus();
        return super.callOnClick();
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
    }

    public EditText getEditText() {
        return editText;
    }
}
