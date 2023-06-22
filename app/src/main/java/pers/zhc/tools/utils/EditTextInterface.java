package pers.zhc.tools.utils;

import android.text.Editable;
import androidx.annotation.StringRes;

public interface EditTextInterface {
    Editable getText();

    void setText(@StringRes int resId);

    void setText(CharSequence text);

    void setHint(CharSequence text);

    void setHint(@StringRes int hintRes);
}
