package pers.zhc.tools.utils;

/**
 * @author bczhc
 */
public class NotifyLatch {
    public synchronized void await() {
        try {
            this.wait();
        } catch (InterruptedException ignored) {
        }
    }

    public synchronized void unlatch() {
        this.notify();
    }
}
