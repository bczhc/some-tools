package pers.zhc.u.util;

/**
 * @author bczhc
 */
public class GuardedObject<T> {
    private T response;

    public synchronized T get() throws InterruptedException {
        while (response == null) {
            this.wait();
        }
        return response;
    }

    public synchronized T get(long timeout) throws InterruptedException {
        final long begin = System.currentTimeMillis();
        long passedTime = 0L;
        while (response == null) {
            long delay = timeout - passedTime;
            if (delay <= 0) {
                break;
            }
            this.wait(delay);
            passedTime = System.currentTimeMillis() - begin;
        }
        return response;
    }

    public synchronized void put(T o) {
        this.response = o;
        this.notifyAll();
    }
}
