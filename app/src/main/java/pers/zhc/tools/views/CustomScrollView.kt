package pers.zhc.tools.views

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView

class CustomScrollView(context: Context) : ScrollView(context) {
    private var startX = 0f
    private var startY = 0f
    private var isIntercepted = false
    private var hasIntercepted = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isIntercepted = false
                hasIntercepted = false
                Log.i("fdb-test", "down")
            }
            MotionEvent.ACTION_MOVE -> {
                if (!hasIntercepted) {
                    val deltaX = ev.x - startX
                    val deltaY = ev.y - startY
                    val angle = Math.toDegrees(Math.atan2(Math.abs(deltaX).toDouble(), Math.abs(deltaY).toDouble()))

                    if(angle != 0.0) {
                        hasIntercepted = true
                    }
                    Log.i("fdb-test", "Angle: $angle")
                    if (angle > 25) {
                        isIntercepted = true
                        Log.i("fdb-test", "move and Touch event intercepted")
                        return false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasIntercepted = false
                Log.i("fdb-test", "up")
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isIntercepted) {
            Log.i("fdb-test", "move and Touch event intercepted")
            return false
        }
        return super.onTouchEvent(ev)
    }
}