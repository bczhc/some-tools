package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;

public class KeyEventResolver {
    private final KeyEventResolverCallback callback;
    private boolean holdShift = false;
    private boolean holdCtrl = false;

    public KeyEventResolver(KeyEventResolverCallback callback) {
        this.callback = callback;
    }

    public boolean isHoldShift() {
        return holdShift;
    }

    public boolean isHoldCtrl() {
        return holdCtrl;
    }

    public boolean onKeyDown(KeyEvent event) {
        WubiIME.InputRange inputRange = WubiIME.checkInputRange(event.getKeyCode());
        if (inputRange == WubiIME.InputRange.SHIFT) {
            callback.onShift(event);
            holdShift = true;
        }
        if (inputRange == WubiIME.InputRange.CTRL) {
            callback.onCtrl(event);
            holdCtrl = true;
        }
        return callback.onKey(event);
    }

    public boolean onKeyUp(KeyEvent event) {
        WubiIME.InputRange inputRange = WubiIME.checkInputRange(event.getKeyCode());
        if (inputRange == WubiIME.InputRange.SHIFT) holdShift = false;
        if (inputRange == WubiIME.InputRange.CTRL) holdCtrl = false;
        return callback.onKey(event);
    }
}