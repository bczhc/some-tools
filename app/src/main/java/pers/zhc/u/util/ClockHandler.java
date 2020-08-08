package pers.zhc.u.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClockHandler<MsgType> {
    private final Runnable runnable;
    private final ExecutorService es;
    private boolean start;
    private ParamReference<MsgType> paramReference;

    public ClockHandler(HandlerCallback<MsgType> callback, long periodMillis) {
        es = Executors.newFixedThreadPool(1);
        runnable = () -> {
            while (start) {
                callback.callback(this.paramReference.param);
                try {
                    Thread.sleep(periodMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        paramReference = new ParamReference<>();
    }

    public void start() {
        this.start = true;
        es.execute(runnable);
    }

    public ParamReference<MsgType> getParamReference() {
        return this.paramReference;
    }

    public void stop() {
        this.start = false;
        es.shutdownNow();
    }

    public static class ParamReference<T> {
        public T param;
    }
}
