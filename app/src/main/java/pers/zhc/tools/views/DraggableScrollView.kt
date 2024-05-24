package pers.zhc.tools.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil

/**
 * @author bczhc
 */
class DraggableScrollView : ScrollView {
    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun init() {

    }

    class RightDragView : View {
        private lateinit var mBackgroundPaint: Paint
        private lateinit var mSliderPaint: Paint

        constructor(context: Context?) : this(context, null)

        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
            init(attrs)
        }

        fun init(attrs: AttributeSet?) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.RightDragView)
            val bgColor = ta.getColor(R.styleable.RightDragView_backgroundColor, Color.RED)
            val sliderColor = ta.getColor(R.styleable.RightDragView_sliderColor, Color.BLUE)
            ta.recycle()

            mBackgroundPaint.color = bgColor
            mSliderPaint.color = sliderColor
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawRect(0F, 0F, measuredWidth.toFloat(), measuredHeight.toFloat(), mBackgroundPaint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            invalidate()
            return true
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec)
            val widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec)

            var measuredWidth = 0
            var measuredHeight = 0

            when (widthMeasureMode) {
                MeasureSpec.EXACTLY -> measuredWidth = widthMeasureSize
                MeasureSpec.AT_MOST -> TODO()
                MeasureSpec.UNSPECIFIED -> measuredWidth = DisplayUtil.dip2px(context, 11F)
            }

            when (heightMeasureMode) {
                MeasureSpec.EXACTLY -> measuredHeight = heightMeasureSize
                MeasureSpec.AT_MOST -> TODO()
                MeasureSpec.UNSPECIFIED -> TODO()
            }

            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }
}
