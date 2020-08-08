package pers.zhc.u;

/**
 * @author bczhc
 */
public class Latch {
    private volatile boolean stop = false;

    public void await() {
        for (; ; ) {
            if (stop) {
                break;
            }
        }
    }

    public void suspend() {
        stop = false;
    }

    public void stop() {
        stop = true;
    }
}
