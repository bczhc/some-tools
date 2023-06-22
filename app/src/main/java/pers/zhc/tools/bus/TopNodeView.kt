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
import kotlin.math.min

class TopNodeView : View {
    private lateinit var linePaint: Paint
    private lateinit var circlePaint: Paint
    private lateinit var busMarkDotPaint: Paint

    private var busState: BusState? = null
    private var busMarkDotCount = 0

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        context!!
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TopLineNodeView)
        busMarkDotCount = ta.getInt(R.styleable.TopLineNodeView_busMarkDotCount, 0)
        busState = if (ta.getInt(R.styleable.TopLineNodeView_busState, 0) == 0) {
            BusState.ARRIVED
        } else {
            BusState.ON_ROAD
        }
        ta.recycle()
        init()
    }

    private fun init() {
        linePaint = Paint()
        linePaint.color = ContextCompat.getColor(context, R.color.colorAccent)

        circlePaint = Paint()
        circlePaint.color = Color.GRAY

        busMarkDotPaint = Paint()
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

        val nodeDotRadius = measuredHeight / 4F / 2F
        val lineHeight = nodeDotRadius / 2F
        val busMarkDotRadius = min(
            (measuredWidth / busMarkDotCount) / 2.0,
            measuredHeight / 2.0 - 2 * nodeDotRadius
        ).toFloat()

        // draw the bottom line
        canvas.save()
        canvas.translate(0F, measuredHeight - nodeDotRadius - lineHeight / 2F)
        canvas.drawRect(0F, 0F, measuredWidth, lineHeight, linePaint)
        canvas.restore()

        if (busState == null || busState == BusState.ARRIVED) {
            // draw the bottom node circle
            canvas.drawCircle(measuredWidth / 2F, measuredHeight - nodeDotRadius, nodeDotRadius, circlePaint)
        }

        if (busState != null) {
            // draw the bus state mark
            busMarkDotPaint.color = when (busState) {
                BusState.ARRIVED -> ARRIVED_BUS_MARK_COLOR
                BusState.ON_ROAD -> ON_ROAD_BUS_MARK_COLOR
                null -> Color.TRANSPARENT
            }

            for (n in 0 until busMarkDotCount) {
                val a = measuredWidth / busMarkDotCount.toFloat()
                canvas.drawCircle(
                    a * n.toFloat() + a / 2F,
                    measuredHeight / 2F,
                    busMarkDotRadius,
                    busMarkDotPaint
                )
            }
        }
    }

    fun setBusState(state: BusState) {
        this.busState = state
        invalidate()
    }

    fun setBusMarkDotCount(count: Int) {
        this.busMarkDotCount = count
        invalidate()
    }

    fun getBusMarkDotCount() = this.busMarkDotCount

    enum class BusState {
        ARRIVED,
        ON_ROAD
    }

    companion object {
        private const val ARRIVED_BUS_MARK_COLOR = Color.GREEN
        private const val ON_ROAD_BUS_MARK_COLOR = Color.RED
    }
}
