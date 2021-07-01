package pers.zhc.tools.utils;

/**
 * @author bczhc
 */
public abstract class RefCountHolder<T> {
    private final T obj;
    private int refCount = 0;

    public RefCountHolder(T obj) {
        this.obj = obj;
    }

    public T getRef() {
        ++refCount;
        return obj;
    }

    public void releaseRef() {
        --refCount;
        if (refCount == 0) {
            onClose(obj);
        }
    }

    public int getRefCount() {
        return refCount;
    }

    /**
     * Return if the `refCount` is 0; if abandoned, this holder should be re-created
     * @return if is abandoned
     */
    public boolean isAbandoned() {
        return refCount == 0;
    }

    public abstract void onClose(T obj);
}
