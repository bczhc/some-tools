package pers.zhc.tools.bus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil

class TopLineNodeView : View {
    private lateinit var linePaint: Paint
    private lateinit var circlePaint: Paint
    private lateinit var busMarkCirclePaint: Paint

    private var busState: BusState? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        linePaint = Paint()
        linePaint.color = ContextCompat.getColor(context, R.color.colorAccent)

        circlePaint = Paint()
        circlePaint.color = Color.GRAY

        busMarkCirclePaint = Paint()
    }

    private fun measure(measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        val measuredSize: Int
        val defaultSize = DisplayUtil.dip2px(context, 32F)
        measuredSize = when (mode) {
            MeasureSpec.EXACTLY -> {
                size
            }
            MeasureSpec.AT_MOST -> {
                defaultSize.coerceAtMost(size)
            }
            MeasureSpec.UNSPECIFIED -> {
                defaultSize
            }
            else -> {
                0
            }
        }
        return measuredSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = measure(widthMeasureSpec)
        val measuredHeight = measure(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!

        val measuredWidth = measuredWidth.toFloat()
        val measuredHeight = measuredHeight.toFloat()

        val circleRadius = measuredHeight / 4F / 2F
        val lineHeight = circleRadius / 2F

        // draw the bottom line
        canvas.save()
        canvas.translate(0F, measuredHeight - circleRadius - lineHeight / 2F)
        canvas.drawRect(0F, 0F, measuredWidth, lineHeight, linePaint)
        canvas.restore()

        // draw the bottom circle
        canvas.drawCircle(measuredWidth / 2F, measuredHeight - circleRadius, circleRadius, circlePaint)

        if (busState != null) {
            // draw the bus state mark
            val busMarkCircleRadius = measuredHeight / 2F / 2F
            busMarkCirclePaint.color = when (busState) {
                BusState.ARRIVED -> ARRIVED_BUS_MARK_COLOR
                BusState.ON_ROAD -> ON_ROAD_BUS_MARK_COLOR
                null -> Color.TRANSPARENT
            }
            canvas.drawCircle(measuredWidth / 2F, measuredHeight / 2F, busMarkCircleRadius, busMarkCirclePaint)
        }
    }

    fun setBusState(state: BusState) {
        this.busState = state
        invalidate()
    }

    enum class BusState {
        ARRIVED,
        ON_ROAD
    }

    companion object {
        private const val ARRIVED_BUS_MARK_COLOR = Color.GREEN
        private const val ON_ROAD_BUS_MARK_COLOR = Color.RED
    }
}