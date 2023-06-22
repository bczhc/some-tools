package pers.zhc.tools.utils;

import org.jetbrains.annotations.Contract;

/**
 * @author bczhc
 */
public abstract class RefCountHolder<T> {
    private final T obj;
    private int refCount = 0;

    @Contract(pure = true)
    public RefCountHolder(T obj) {
        this.obj = obj;
    }

    public T newRef() {
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
     *
     * @return if is abandoned
     */
    public boolean isAbandoned() {
        return refCount == 0;
    }

    public abstract void onClose(T obj);
}
