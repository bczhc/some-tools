package pers.zhc.tools.fourierseries

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class DrawingActivity: BaseActivity() {
    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawingView = DrawingView(this)

        setContentView(drawingView)
    }

    override fun finish() {
        ToastUtils.show(this, drawingView.points.size.toString())
        super.finish()
    }
}