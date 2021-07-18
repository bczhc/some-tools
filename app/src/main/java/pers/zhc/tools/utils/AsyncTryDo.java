package pers.zhc.tools.utils;

import org.jetbrains.annotations.NotNull;

/**
 * @author bczhc
 */
public class AsyncTryDo {
    private volatile boolean done = true;
    private final Notifier notifier = () -> done = true;

    public void tryDo(@NotNull Callback callback) {
        if (done) {
            done = false;
            callback.callback(this, notifier);
        }
    }

    public void reset() {
        done = true;
    }

    public interface Notifier {
        void finish();
    }

    public interface Callback {
        void callback(@NotNull AsyncTryDo self, @NotNull Notifier notifier);
    }
}
