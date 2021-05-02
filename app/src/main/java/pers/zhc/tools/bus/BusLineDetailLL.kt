package pers.zhc.tools.bus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.bus_line_detail_station_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil

/**
 * @author bczhc
 */
class BusLineDetailLL : LinearLayout {
    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        this.orientation = HORIZONTAL
    }

    fun addStation(stationName: String) {
        val stationView = getStationView(this.childCount + 1, stationName)
        this.addView(stationView)
    }

    private fun getStationView(ordinal: Int, stationName: String): View? {
        val inflate = View.inflate(context, R.layout.bus_line_detail_station_view, null)
        inflate.ordinal_tv!!.text = context.getString(R.string.bus_line_detail_station_ordinal_tv, ordinal)
        inflate.station_name_tv!!.text = stationName
        return inflate
    }

    class TopLineNodeView : View {
        private lateinit var linePaint: Paint
        private lateinit var circlePaint: Paint

        constructor(context: Context?) : this(context, null)

        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
            init()
        }

        private fun init() {
            linePaint = Paint()
            linePaint.color = ContextCompat.getColor(context, R.color.colorAccent)
            circlePaint = Paint()
            circlePaint.color = Color.GRAY
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

            canvas.save()
            canvas.translate(0F, measuredHeight / 2F - lineHeight / 2F)
            canvas.drawRect(0F, 0F, measuredWidth, lineHeight, linePaint)
            canvas.restore()

            canvas.drawCircle(measuredWidth / 2F, measuredHeight / 2F, circleRadius, circlePaint)
        }
    }
}