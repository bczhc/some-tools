package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;

public interface KeyEventResolverCallback {
    boolean onKey(KeyEvent event);

    void onShift(KeyEvent event);

    void onCtrl(KeyEvent event);
}
