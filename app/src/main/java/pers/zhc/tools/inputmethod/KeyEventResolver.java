package pers.zhc.tools.inputmethod;

import android.view.KeyEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class KeyEventResolver {
    private final KeyEventResolverCallback callback;

    @Contract(pure = true)
    public KeyEventResolver(KeyEventResolverCallback callback) {
        this.callback = callback;
    }

    public boolean onKeyDown(@NotNull KeyEvent event) {
        return callback.onKey(event);
    }

    public boolean onKeyUp(KeyEvent event) {
        return callback.onKey(event);
    }
}