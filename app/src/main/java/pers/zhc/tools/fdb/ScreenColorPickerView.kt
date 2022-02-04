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
    private val borderWidth = DisplayUtil.cm2px(context, 1.5F / 10F).toFloat()
    private var bitmap: Bitmap? = null
    private val bitmapPaint = Paint()
    private var pointX = 0F
    private var pointY = 0F
    private val innerEdgeLength = DisplayUtil.cm2px(context, 2F)
    private val edgeWidth = 2F
    private var color: Int = Color.TRANSPARENT
    private val edgePaint = Paint()
    private val pointWidth = 6F

    private var onScreenSizeChangedListener: OnScreenSizeChangedListener? = null

    private fun init() {
        pointPaint.apply {
            color = Color.RED
            strokeWidth = pointWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        borderPaint.apply {
            color = Color.TRANSPARENT
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        edgePaint.apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = edgeWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        pointPaint.color = getInvertColor()
        borderPaint.color = color
        edgePaint.color = getInvertColor()

        if (transparent) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            return
        }

        canvas.translate(edgeWidth / 2F, edgeWidth / 2F)
        canvas.translate(borderWidth, borderWidth)
        canvas.translate(innerEdgeLength.toFloat() / 2F, innerEdgeLength.toFloat() / 2F)
        canvas.translate(-pointX, -pointY)
        // inner bitmap content
        bitmap?.let {
            canvas.drawBitmap(it, 0F, 0F, bitmapPaint)
        }

        canvas.translate(pointX, pointY)

        // center point
        canvas.drawPoint(0F, 0F, pointPaint)

        canvas.translate(-innerEdgeLength.toFloat() / 2F, -innerEdgeLength.toFloat() / 2F)
        canvas.translate(-borderWidth, -borderWidth)
        // color border filled area
        canvas.drawRect(0F, 0F, innerEdgeLength.toFloat() + 2F * borderWidth, borderWidth, borderPaint)
        canvas.drawRect(0F, 0F, borderWidth, innerEdgeLength.toFloat() + 2F * borderWidth, borderPaint)
        canvas.translate(innerEdgeLength.toFloat() + 2F * borderWidth, innerEdgeLength.toFloat() + 2F * borderWidth)
        canvas.drawRect(0F, 0F, -(innerEdgeLength.toFloat() + 2F * borderWidth), -borderWidth, borderPaint)
        canvas.drawRect(0F, 0F, -borderWidth, -(innerEdgeLength.toFloat() + 2F * borderWidth), borderPaint)

        canvas.translate(
            -(innerEdgeLength.toFloat() + 2F * borderWidth),
            -(innerEdgeLength.toFloat() + 2F * borderWidth)
        )
        // color border stroke lines
        canvas.drawRect(
            0F,
            0F,
            innerEdgeLength.toFloat() + 2F * borderWidth,
            innerEdgeLength.toFloat() + 2F * borderWidth,
            edgePaint
        )
    }

    private var lastWindowWidth = 0
    private var lastWindowHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val edgeLength = innerEdgeLength + borderWidth.toInt() * 2 + (edgeWidth / 2F).toInt() + pointWidth.toInt()
        setMeasuredDimension(edgeLength, edgeLength)

        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        if (w != lastWindowWidth || h != lastWindowHeight) {
            onScreenSizeChangedListener?.invoke(w, h)
        }
        lastWindowWidth = w
        lastWindowHeight = h
    }

    fun setColor(color: Int) {
        this.color = color
        invalidate()
    }

    private fun getInvertColor(): Int {
        return if (color != Color.TRANSPARENT) {
            ColorUtils.invertColor(color)
        } else Color.BLACK
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
        this.pointY = pointY
        invalidate()
    }

    fun getColor(): Int? {
        bitmap ?: return null
        bitmap!!.let {
            val y = pointY.toInt()
            val x = pointX.toInt()
            return if (x < 0 || x > it.width || y < 0 || y > it.height) {
                null
            } else {
                this.bitmap!!.getPixel(x, y)
            }
        }
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