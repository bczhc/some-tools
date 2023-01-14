package pers.zhc.tools.app

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.colorpicker.ScreenColorPickerDemoActivity
import pers.zhc.tools.databinding.ToolsActivityMainBinding
import pers.zhc.tools.test.*
import pers.zhc.tools.test.malloctest.MAllocTest
import pers.zhc.tools.test.signals.SignalTest

/**
 * @author bczhc
 */
class TestListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ToolsActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        val recyclerView = bindings.recyclerView

        recyclerView.layoutManager = LinearLayoutManager(this)

        val list = listOf(
            ActivityItem(R.string.test, Demo::class.java),
            ActivityItem(R.string.sensor_test, SensorTest::class.java),
            ActivityItem(R.string.crash_test, CrashTest::class.java),
            ActivityItem(R.string.m_alloc_test, MAllocTest::class.java),
            ActivityItem(R.string.tts_test, TTS::class.java),
            ActivityItem(R.string.drawing_board_test, DrawingBoardTest::class.java),
            ActivityItem(R.string.signal_test_label, SignalTest::class.java),
            ActivityItem(R.string.screen_color_picker_label, ScreenColorPickerDemoActivity::class.java),
        )

        recyclerView.adapter = AppMenuAdapter(this, list)
    }
}