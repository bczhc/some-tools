package pers.zhc.tools.fourierseries

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import pers.zhc.tools.BaseView
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil
import java.util.*

/**
 * @author bczhc
 */
class DrawingView : BaseView {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private val paint = Paint()
    private var path: Path? = null
    val points = LinkedList<InputPoint>()
    private val axesPaint = Paint()

    init {
        paint.apply {
            strokeWidth = DisplayUtil.dip2px(context, 1.0F).toFloat()
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.highContrastMain)
        }
        axesPaint.apply {
            strokeWidth = 0.0F
            color = Color.GRAY
            style = Paint.Style.STROKE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path = Path()
                path!!.moveTo(x, y)
                points.clear()
            }

            MotionEvent.ACTION_MOVE -> {
                path!!.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                path!!.close()
            }
        }
        points.add(InputPoint(x - width / 2F, -y + height / 2F))
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        // draw the axes
        canvas.drawLine(0F, height / 2F, width, height / 2F, axesPaint)
        canvas.drawLine(width / 2F, 0F, width / 2F, height, axesPaint)

        path?.also {
            canvas.drawPath(it, paint)
        }
    }

    private fun measure(measureSpec: Int): Int {
        val size = MeasureSpec.getSize(measureSpec)

        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> {
                size
            }

            MeasureSpec.AT_MOST -> {
                size
            }

            MeasureSpec.UNSPECIFIED -> {
                throw RuntimeException("Please specify the size of the view")
            }

            else -> {
                0
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec))
    }
}
