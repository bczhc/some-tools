package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;

public interface KeyEventResolverCallback {
    void onKeyDown(KeyEvent event);

    void onKeyDownWithShift(KeyEvent event);

    void onkeyUp(KeyEvent event);

    void onShift(KeyEvent event);
}
