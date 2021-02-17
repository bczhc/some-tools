package pers.zhc.tools.floatingdrawing;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * @author bczhc
 */
public class LongClickResolver {
    private final GestureDetector gd;

    public LongClickResolver(Context ctx, LongClickCallbackInterface callback) {
        gd = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                callback.click();
            }
        });
    }

    void onTouch(MotionEvent e) {
        gd.onTouchEvent(e);
    }
}
