package pers.zhc.tools.fourierseries

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import kotlinx.android.synthetic.main.fourier_series_epicycles_drawing_activity.*
import kotlinx.android.synthetic.main.fourier_series_epicycles_drawing_option_speed_dialog.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.PopupMenuUtil

/**
 * @author bczhc
 */
class EpicycleDrawingActivity : BaseActivity() {
    private lateinit var drawingView: EpicycleDrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fourier_series_epicycles_drawing_activity)

        val container = container!!
        val optionButton = option_btn!!

        drawingView = EpicycleDrawingView(this, FourierSeriesActivity.epicycleData)
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
        val view = View.inflate(this, R.layout.fourier_series_epicycles_drawing_option_speed_dialog, null)
        Dialog(this).apply {
            setContentView(view)
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
        }.show()

        val speedSlider = view.speed_slider!!
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