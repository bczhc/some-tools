package pers.zhc.tools.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import pers.zhc.tools.BaseView

/**
 * @author bczhc
 */
class ColorView : BaseView {
    private var color: Int = 0

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setColor(color: Int) {
        this.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.drawColor(color)
    }
}
