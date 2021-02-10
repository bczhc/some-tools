package pers.zhc.tools.floatingdrawing;

import android.view.MotionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author bczhc
 */
public class LongClickResolver {
    private float prevX, prevY;
    private final int delay;
    private long downTime = 0;
    private boolean shouldPerformLongClick = true;
    private final LongClickCallbackInterface callback;
    private boolean done = false;

    public LongClickResolver(int delay, LongClickCallbackInterface callback) {
        this.delay = delay;
        this.callback = callback;
    }

    public LongClickResolver(LongClickCallbackInterface callback) {
        this(500, callback);
    }

    private void reset() {
        shouldPerformLongClick = true;
        done = false;
    }

    public void onTouch(@NotNull MotionEvent event) {
        int i = 2;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                prevX = event.getX();
                prevY = event.getY();
                downTime = System.currentTimeMillis();
                reset();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                if (Math.abs(x - prevX) > i || Math.abs(y - prevY) > i) {
                    shouldPerformLongClick = false;
                }
                if (System.currentTimeMillis() - downTime >= delay && shouldPerformLongClick && !done) {
                    callback.click();
                    done = true;
                }
                prevX = x;
                prevY = y;
                break;
            default:
        }
    }
}
