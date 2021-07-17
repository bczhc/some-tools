package pers.zhc.tools.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bczhc
 */
public class Messenger<M> {
    private volatile @Nullable
    M msg;
    private volatile boolean stop = false;

    public void set(@Nullable M m) {
        msg = m;
    }

    public void start(@NotNull MsgInterface<M> callback) {
        Notifier notifier = () -> {
            synchronized (this) {
                this.notify();
            }
        };

        while (!stop) {
            callback.msg(this, msg, notifier);
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        stop = true;
    }

    public interface Notifier {
        void finish();
    }

    public interface MsgInterface<M> {
        void msg(@NotNull Messenger<M> self, M msg, @NotNull Notifier notifier);
    }
}
