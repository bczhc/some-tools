package pers.zhc.tools.fourierseries

import android.os.Bundle
import pers.zhc.tools.BaseActivity

/**
 * @author bczhc
 */
class EpicycleDrawingActivity: BaseActivity() {
    private lateinit var view: EpicycleDrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = EpicycleDrawingView(this, FourierSeriesActivity.epicycleData)
        view.startAnimation()
        setContentView(view)
    }

    override fun finish() {
        view.stopAnimation()
        super.finish()
    }
}