package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import pers.zhc.tools.BaseViewGroup

/**
 * @author bczhc
 */
open class WrapLayout : BaseViewGroup {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 1) {
            val view = getChildAt(0)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (childCount == 1) {
            val view = getChildAt(0)
            view.measure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(view.measuredWidth, view.measuredHeight)
        }
    }

    fun setView(view: View) {
        if (this.childCount >= 1) {
            this.removeAllViews()
        }
        this.addView(view)
    }
}
