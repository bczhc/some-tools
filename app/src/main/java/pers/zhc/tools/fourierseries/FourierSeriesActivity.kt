package pers.zhc.tools.fourierseries

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.fourier_series_main.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class FourierSeriesActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fourier_series_main)

        val drawButton = draw_btn!!
        val calcButton = calc_btn!!
        val startButton = start_btn!!
        
        drawButton.setOnClickListener {
            startActivity(Intent(this, DrawingActivity::class.java))
        }
        calcButton.setOnClickListener { 
            
        }
        startButton.setOnClickListener {
            startActivity(Intent(this, EpicycleDrawingActivity::class.java))
        }
    }
}