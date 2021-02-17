package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;
import org.jetbrains.annotations.NotNull;

public class KeyEventResolver {
    private final KeyEventResolverCallback callback;

    public KeyEventResolver(KeyEventResolverCallback callback) {
        this.callback = callback;
    }

    public boolean onKeyDown(@NotNull KeyEvent event) {
        WubiIME.InputRange inputRange = WubiIME.checkInputRange(event.getKeyCode());
        if (inputRange == WubiIME.InputRange.SHIFT) {
            callback.onShift(event);
        }
        if (inputRange == WubiIME.InputRange.CTRL) {
            callback.onCtrl(event);
        }
        return callback.onKey(event);
    }

    public boolean onKeyUp(KeyEvent event) {
        return callback.onKey(event);
    }
}