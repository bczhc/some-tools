package pers.zhc.tools.utils;

import android.support.annotation.StringRes;
import android.text.Editable;

public interface EditTextInterface {
    Editable getText();

    void setText(@StringRes int resId);

    void setText(CharSequence text);

    void setHint(CharSequence text);

    void setHint(@StringRes int hintRes);
}
