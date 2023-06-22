package pers.zhc.tools.fourierseries

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.FourierSeriesEpicyclesDrawingActivityBinding
import pers.zhc.tools.databinding.FourierSeriesEpicyclesDrawingOptionSpeedDialogBinding
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.PopupMenuUtil
import pers.zhc.tools.utils.setBaseLayoutSizeMM

/**
 * @author bczhc
 */
class EpicycleDrawingActivity : BaseActivity() {
    private lateinit var drawingView: EpicycleDrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = FourierSeriesEpicyclesDrawingActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val container = bindings.container
        val optionButton = bindings.optionBtn

        drawingView = EpicycleDrawingView(this, FourierSeriesActivity.epicycleData).apply {
            this.setBaseLayoutSizeMM()
        }
        container.addView(drawingView)
        drawingView.startAnimation()

        optionButton.setOnClickListener {
            val menu = PopupMenuUtil.create(this, it, R.menu.fourier_series_epicycles_drawing_options)
            menu.show()
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.speed -> {
                        showSpeedDialog()
                    }

                    R.id.reset_path -> {
                        drawingView.resetPath()
                    }

                    else -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun showSpeedDialog() {
        val bindings = FourierSeriesEpicyclesDrawingOptionSpeedDialogBinding.inflate(layoutInflater)
        val view = bindings.root
        Dialog(this).apply {
            setContentView(view)
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
        }.show()

        val speedSlider = bindings.speedSlider
        speedSlider.value = (drawingView.tIncrement * 100.0).toFloat()
        speedSlider.addOnChangeListener { _, value, _ ->
            drawingView.tIncrement = value.toDouble() / 100.0
        }
    }

    override fun finish() {
        drawingView.stopAnimation()
        super.finish()
    }
}
