package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import pers.zhc.tools.BaseViewGroup

/**
 * @author bczhc
 */
open class WrapLayout : BaseViewGroup {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 1) {
            val child = getChildAt(0)
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

    private fun getChildMeasureSpec(childSize: Int, parentConstraintSize: Int): Int {
        return when (childSize) {
            WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            MATCH_PARENT -> MeasureSpec.makeMeasureSpec(parentConstraintSize, MeasureSpec.EXACTLY)
            else -> MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (childCount != 1) {
            return
        }
        val child = getChildAt(0)
        val childWidth = child.layoutParams.width
        val childHeight = child.layoutParams.height

        val parentWidthSize = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeightSize = MeasureSpec.getSize(heightMeasureSpec)

        val childWidthMeasureSpec = getChildMeasureSpec(childWidth, parentWidthSize)
        val childHeightMeasureSpec = getChildMeasureSpec(childHeight, parentHeightSize)

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)

        val measuredWidth = View.resolveSize(child.measuredWidth, widthMeasureSpec)
        val measuredHeight = View.resolveSize(child.measuredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }


    fun setView(view: View) {
        if (this.childCount >= 1) {
            this.removeAllViews()
        }
        this.addView(view)
    }
}
