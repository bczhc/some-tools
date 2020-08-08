package pers.zhc.u;

import pers.zhc.u.util.HandlerCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CanDoHandler<MsgType> implements Runnable {
    private MsgType param;
    private boolean aDo = false;
    private boolean start;
    private final HandlerCallback<MsgType> handlerCallback;
    private final ExecutorService es;

    public CanDoHandler(HandlerCallback<MsgType> handlerCallback) {
        this.handlerCallback = handlerCallback;
        es = Executors.newFixedThreadPool(1);
    }

    @Override
    public void run() {
        while (this.start) {
            if (aDo) {
                handlerCallback.callback(param);
                aDo = false;
            }
        }
    }

    public void stop() {
        this.start = false;
        es.shutdownNow();
    }

    public void start() {
        this.start = true;
        es.execute(this);
    }

    public void push(MsgType msg) {
        this.param = msg;
        this.aDo = true;
    }
}