package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RadioGroup

/**
 * @author bczhc
 */
class MyRadioGroup : RadioGroup {
    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val selfWidth = measuredWidth
        val selfHeight = measuredHeight

        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(selfWidth, MeasureSpec.AT_MOST)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(selfHeight, MeasureSpec.AT_MOST)
        var restWidth: Int
        var leftStart = 0
        var topStart = 0
        var columnMaxHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec, heightMeasureSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            restWidth = selfWidth - leftStart
            if (childWidth > restWidth) {
                // new column
                topStart += columnMaxHeight
                columnMaxHeight = 0
                leftStart = 0
            }
            if (childHeight > columnMaxHeight) columnMaxHeight = childHeight
            child.layout(leftStart, topStart, leftStart + childWidth, topStart + childHeight)
            leftStart += childWidth
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredWidth = 0
        var measuredHeight = 0

        val widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec)

        when (widthMeasureMode) {
            MeasureSpec.AT_MOST -> {
                TODO()
            }
            MeasureSpec.EXACTLY -> {
                measuredWidth = widthMeasureSize
            }
            MeasureSpec.UNSPECIFIED -> {
                TODO()
            }
        }

        when (heightMeasureMode) {
            MeasureSpec.AT_MOST -> {
                TODO()
            }
            MeasureSpec.EXACTLY -> {
                measuredHeight = heightMeasureSize
            }
            MeasureSpec.UNSPECIFIED -> {
                TODO()
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }
}