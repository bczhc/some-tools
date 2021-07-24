package pers.zhc.tools.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import pers.zhc.tools.floatingdrawing.MyCanvas
import pers.zhc.tools.utils.GestureResolver

/**
 * @author bczhc
 */
class ScalableImageView : View {
    private lateinit var mGestureResolver: GestureResolver
    private var srcBitmap: Bitmap? = null
    private var mBitmap: Bitmap? = null
    private lateinit var mCanvas: MyCanvas
    private lateinit var mBitmapPaint: Paint

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mBitmapPaint = Paint()

        mGestureResolver = GestureResolver(object : GestureResolver.GestureInterface {
            override fun onTwoPointsScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
                mCanvas.translateReal(distanceX, distanceY)
            }

            override fun onTwoPointsZoom(
                firstMidPointX: Float,
                firstMidPointY: Float,
                midPointX: Float,
                midPointY: Float,
                firstDistance: Float,
                distance: Float,
                scale: Float,
                dScale: Float,
                event: MotionEvent?
            ) {
                mCanvas.scaleReal(dScale, midPointX, midPointY)
            }

            override fun onTwoPointsUp(event: MotionEvent) {
            }

            override fun onTwoPointsDown(event: MotionEvent) {
            }

            override fun onTwoPointsPress(event: MotionEvent) {
            }

            override fun onOnePointScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
            }
        })
    }

    fun setBitmap(src: Bitmap) {
        srcBitmap = src
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mGestureResolver.onTouch(event)
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        srcBitmap?.let { mCanvas.drawBitmap(it, 0F, 0F, mBitmapPaint) }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        mBitmap?.let {
            canvas!!.drawBitmap(it, 0F, 0F, mBitmapPaint)
        }
    }

    private fun measure(measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)

        return when (mode) {
            MeasureSpec.AT_MOST -> {
                size.coerceAtMost(512)
            }
            MeasureSpec.EXACTLY -> {
                size
            }
            MeasureSpec.UNSPECIFIED -> {
                512
            }
            else -> 0
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measure(widthMeasureSpec)
        val measuredHeight = measure(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)

        if (mBitmap == null) {
            // init
            mBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            mCanvas = MyCanvas(mBitmap!!)
            srcBitmap?.let { mCanvas.drawBitmap(it, 0F, 0F, mBitmapPaint) }
        }
    }
}