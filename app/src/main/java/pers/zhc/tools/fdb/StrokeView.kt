package pers.zhc.tools.fdb

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import pers.zhc.tools.BaseView
import pers.zhc.tools.floatingdrawing.PaintView
import kotlin.math.ceil

/**
 * @author bczhc
 */
class StrokeView : BaseView {
    private var blurRadius = 0F
    private val mPaint = Paint()

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private val mPath = Path()

    override fun onDraw(canvas: Canvas) {
        val strokeWidth = mPaint.strokeWidth
        val offset = strokeWidth / 2F + blurRadius
        canvas.translate(offset, offset)

        mPath.reset()
        mPath.moveTo(0F, 0F)
        mPath.lineTo(0F, 0F)
        canvas.drawPath(mPath, mPaint)
    }

    fun setColor(color: Int) {
        mPaint.color = color
        invalidate()
    }

    /**
     * the [width] is displayed width
     */
    fun setWidth(width: Float) {
        mPaint.strokeWidth = width
        invalidate()
        requestLayout()
    }

    private fun setBlurRadius(radius: Float) {
        if (radius == 0F) {
            mPaint.maskFilter = null
        } else {
            mPaint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        this.blurRadius = radius
        invalidate()
        requestLayout()
    }

    var strokeHardness: Float = 100F
    set(value) {
        setBlurRadius(PaintView.toBlurRadius(mPaint.strokeWidth, value))
        field = value
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val diameter = ceil((blurRadius * 2F + mPaint.strokeWidth).toDouble()).toInt()
        setMeasuredDimension(diameter, diameter)
    }
}