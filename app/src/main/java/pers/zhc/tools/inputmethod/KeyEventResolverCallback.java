package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;

public interface KeyEventResolverCallback {
    boolean onKey(KeyEvent event);
}
