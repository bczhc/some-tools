package pers.zhc.tools.fdb

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import pers.zhc.tools.BaseView
import pers.zhc.tools.utils.ColorUtils
import pers.zhc.tools.utils.DisplayUtil

/**
 * @author bczhc
 */
class ScreenColorPickerView : BaseView {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private var transparent = false
    private val pointPaint = Paint()
    private val borderPaint = Paint()
    private val borderWidth = 8F
    private var bitmap: Bitmap? = null
    private val bitmapPaint = Paint()
    private var pointX = 0F
    private var pointY = 0F

    private var onScreenSizeChangedListener: OnScreenSizeChangedListener? = null

    private fun init() {
        pointPaint.color = Color.RED
        pointPaint.strokeWidth = 5F

        borderPaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        if (transparent) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            return
        }

        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        bitmap?.let {
            canvas.drawBitmap(it, -(pointX - width / 2F), -(pointY - height / 2F), bitmapPaint)
        }

        canvas.drawRect(0F, 0F, width, borderWidth, borderPaint)
        canvas.drawRect(0F, height - borderWidth, width, height, borderPaint)
        canvas.drawRect(0F, 0F, borderWidth, height, borderPaint)
        canvas.drawRect(width - borderWidth, 0F, width, height, borderPaint)

        canvas.drawPoint(width / 2F, height / 2F, pointPaint)
    }

    private var lastWindowWidth = 0
    private var lastWindowHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val radius = DisplayUtil.dip2px(context, DisplayUtil.cm2dp(2F).toFloat())
        setMeasuredDimension(radius, radius)

        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        if (w != lastWindowWidth || h != lastWindowHeight) {
            onScreenSizeChangedListener?.invoke(w, h)
        }
        lastWindowWidth = w
        lastWindowHeight = h
    }

    fun setColor(color: Int) {
        borderPaint.color = color
        pointPaint.color = ColorUtils.invertColor(color)
        invalidate()
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        return bitmap
    }

    fun updatePosition(pointX: Float, pointY: Float) {
        this.pointX = pointX
        this.pointY  = pointY
        invalidate()
    }

    fun getColor(): Int? {
        this.bitmap ?: return null
        return this.bitmap!!.getPixel(pointX.toInt(), pointY.toInt())
    }

    fun setIsTransparent(transparent: Boolean) {
        this.transparent = transparent
        invalidate()
    }

    fun setOnScreenSizeChangedListener(listener: OnScreenSizeChangedListener?) {
        this.onScreenSizeChangedListener = listener
    }
}

typealias OnScreenSizeChangedListener = (width: Int, height: Int) -> Unit