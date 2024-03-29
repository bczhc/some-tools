package pers.zhc.tools.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.widget.ScrollView
import kotlin.math.abs
import kotlin.math.atan2

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
            }

            MotionEvent.ACTION_MOVE -> {
                if (!hasIntercepted) {
                    val deltaX = ev.x - startX
                    val deltaY = ev.y - startY
                    val angle = Math.toDegrees(atan2(abs(deltaX).toDouble(), abs(deltaY).toDouble()))

                    if (angle != 0.0) {
                        hasIntercepted = true
                    }
                    Log.i("fdb-test", "Angle: $angle")
                    if (angle > 25) {
                        isIntercepted = true
                        requestDisallowInterceptTouchEvent(true)
                        return false
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasIntercepted = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isIntercepted) {
            requestDisallowInterceptTouchEvent(true)
            return false
        }
        return super.onTouchEvent(ev)
    }
}
