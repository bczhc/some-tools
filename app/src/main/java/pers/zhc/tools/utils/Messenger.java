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
        AtomicBoolean finish = new AtomicBoolean(false);
        Notifier notifier = () -> finish.set(true);

        while (!stop) {
            callback.msg(this, msg, notifier);
            // spin to wait until finished
            //noinspection StatementWithEmptyBody
            while (!finish.get()) ;
            finish.set(false);
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
