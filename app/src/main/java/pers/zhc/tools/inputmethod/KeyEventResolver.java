package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;

public class KeyEventResolver {
    private final KeyEventResolverCallback callback;
    private boolean holdShift = false;

    public KeyEventResolver(KeyEventResolverCallback callback) {
        this.callback = callback;
    }

    public boolean isHoldShift() {
        return holdShift;
    }

    public void onKeyDown(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (WubiIME.checkInputRange(keyCode) == 5/*shift*/) {
            holdShift = true;
            callback.onShift(event);
        } else if (holdShift) callback.onKeyDownWithShift(event);
        callback.onKeyDown(event);
    }

    public void onKeyUp(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (WubiIME.checkInputRange(keyCode) == 5/*shift*/) {
            holdShift = false;
            callback.onShift(event);
        }
        callback.onkeyUp(event);
    }
}