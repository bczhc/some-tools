package pers.zhc.tools.fourierseries

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import pers.zhc.tools.BaseView
import pers.zhc.tools.utils.AsyncTryDo
import pers.zhc.tools.utils.GestureResolver
import pers.zhc.tools.utils.SpinAsyncAwaiter

/**
 * @author bczhc
 */
@SuppressLint("ViewConstructor")
class EpicycleDrawingView(context: Context, private val epicycles: Epicycles) : BaseView(context) {

    private val axesPaint = Paint()
    private val bitmapPaint = Paint()
    private var pathBitmap: Bitmap? = null
    private val epicyclePaint = Paint()
    private var t = 0.0
    private val endPath = Path()
    private val pathPaint = Paint()
    private var transformation: Matrix? = null
    private var run = true

    init {
        axesPaint.apply {
            strokeWidth = 0F
            color = Color.GRAY
            style = Paint.Style.STROKE
        }
        epicyclePaint.apply {
            strokeWidth = 0F
            color = Color.BLACK
            style = Paint.Style.STROKE
        }
        pathPaint.apply {
            strokeWidth = 0F
            color = Color.RED
            style = Paint.Style.STROKE
        }
    }

    private val gestureResolver = GestureResolver(object : GestureResolver.GestureInterface {
        override fun onTwoPointsScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
            transformation!!.postTranslate(distanceX, distanceY)
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
            transformation!!.postScale(dScale, dScale, midPointX, midPointY)
        }

        override fun onTwoPointsUp(event: MotionEvent?) {
        }

        override fun onTwoPointsDown(event: MotionEvent?) {
        }

        override fun onTwoPointsPress(event: MotionEvent?) {
        }

        override fun onTwoPointsRotate(
            event: MotionEvent?,
            firstMidX: Float,
            firstMidY: Float,
            degrees: Float,
            midX: Float,
            midY: Float
        ) {
        }

        override fun onOnePointScroll(distanceX: Float, distanceY: Float, event: MotionEvent?) {
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureResolver.onTouch(event)
        invalidate()
        return true
    }

    private val complexSum = ComplexValue(0.0, 0.0)
    private val tempComplex = ComplexValue(0.0, 0.0)
    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()
        if (pathBitmap == null) {
            pathBitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        }

        // initialize the transformation: (Android canvas coordinate -> mathematics coordinate)
        if (transformation == null) {
            // set up the coordinate transformation matrix
            // to let the drawing look like in a mathematics coordinate
            transformation = Matrix().apply {
                preTranslate(width / 2F, height / 2F)
                preScale(1F, -1F)
            }
        }
        // apply the transformation
        canvas.setMatrix(transformation)

        // draw the axes
        canvas.drawLine(-width / 2F, 0F, width / 2F, 0F, axesPaint)
        canvas.drawLine(0F, height / 2F, 0F, -height / 2F, axesPaint)

        // draw all epicycles
        complexSum.set(0.0, 0.0)
        for (epicycle in epicycles) {
            val centerX = complexSum.re.toFloat()
            val centerY = complexSum.im.toFloat()
            epicycle.evaluate(this.t, tempComplex)

            // line
            canvas.drawLine(
                centerX,
                centerY,
                centerX + tempComplex.re.toFloat(),
                centerY + tempComplex.im.toFloat(),
                epicyclePaint
            )
            // circle
            canvas.drawCircle(
                centerX,
                centerY,
                tempComplex.module().toFloat(),
                epicyclePaint
            )
            complexSum += tempComplex
        }
        // draw the final epicycles' path
        endPath.lineTo(complexSum.re.toFloat(), complexSum.im.toFloat())
        canvas.drawPath(endPath, pathPaint)
    }

    fun measure(measureSpec: Int): Int {
        val size = MeasureSpec.getSize(measureSpec)
        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> {
                size
            }
            MeasureSpec.UNSPECIFIED -> {
                throw RuntimeException("Please specify the size")
            }
            MeasureSpec.AT_MOST -> {
                size
            }
            else -> {
                0
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec))
    }

    fun startAnimation() {
        val asyncTryDo = AsyncTryDo()
        val handler = Handler(Looper.getMainLooper())
        Thread {
            while (this.run) {
                asyncTryDo.tryDo { _, notifier ->
                    handler.post {
                        invalidate()
                        this.t += 0.01
                        notifier.finish()
                    }
                }
            }
        }.start()
    }

    fun stopAnimation() {
        this.run = false
    }
}