package pers.zhc.tools.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import pers.zhc.tools.utils.CanvasTransformer
import pers.zhc.tools.utils.GestureResolver

/**
 * @author bczhc
 */
class ZoomableImageView : View {
    private lateinit var mGestureResolver: GestureResolver
    private var srcBitmap: Bitmap? = null
    private var mBitmap: Bitmap? = null
    private lateinit var mCanvas: Canvas
    private var canvasTransformer: CanvasTransformer? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    var isRotatable = false

    private fun init() {
        mGestureResolver = GestureResolver(object : GestureResolver.GestureInterface {
            override fun onTwoPointsScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
                canvasTransformer!!.absTranslate(distanceX, distanceY)
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
                canvasTransformer!!.absScale(dScale, midPointX, midPointY)
            }

            override fun onTwoPointsRotate(
                event: MotionEvent?,
                firstMidX: Float,
                firstMidY: Float,
                degrees: Float,
                midX: Float,
                midY: Float
            ) {
                if (isRotatable) {
                    canvasTransformer!!.absRotate(degrees, midX, midY)
                }
            }

            override fun onTwoPointsUp(event: MotionEvent) {
            }

            override fun onTwoPointsDown(event: MotionEvent) {
            }

            override fun onTwoPointsPress(event: MotionEvent) {
            }

            override fun onOnePointScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
                canvasTransformer!!.absTranslate(distanceX, distanceY)
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
        srcBitmap?.let { mCanvas.drawBitmap(it, 0F, 0F, null) }
        invalidate()
        return true
    }

    private fun initBitmap() {
        mBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
        val oldMatrix = canvasTransformer?.newMatrix
        canvasTransformer = CanvasTransformer(mCanvas).apply {
            // keep the old transformation
            // TODO: but the desired goal is to keep the center point of the screen "pinned".
            //  That's to say, after the screen rotates, the image center point acts like a pivot.
            matrix = oldMatrix
        }
        srcBitmap?.let { mCanvas.drawBitmap(it, 0F, 0F, null) }
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap == null) {
            // init
            initBitmap()
        }
        mBitmap?.let {
            if (it.width != measuredWidth || it.height != measuredHeight) {
                // WH mismatched; recreate the bitmap
                initBitmap()
            }
        }

        canvas.drawBitmap(mBitmap!!, 0F, 0F, null)
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
    }
}
