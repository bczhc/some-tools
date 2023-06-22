package pers.zhc.tools.fourierseries

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import java.util.*

/**
 * @author bczhc
 */
class DrawingActivity : BaseActivity() {
    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawingView = DrawingView(this)

        setContentView(drawingView)
    }

    override fun finish() {
        points = drawingView.points
        super.finish()
    }

    companion object {
        var points: InputPoints? = null
    }
}

typealias InputPoints = LinkedList<InputPoint>
