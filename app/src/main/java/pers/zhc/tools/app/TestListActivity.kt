package pers.zhc.tools.app

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.tools_activity_main.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.test.*
import pers.zhc.tools.test.malloctest.MAllocTest

/**
 * @author bczhc
 */
class TestListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tools_activity_main)
        val recyclerView = recycler_view!!

        recyclerView.layoutManager = LinearLayoutManager(this)

        val list = listOf(
            ActivityItem(R.string.test, Demo::class.java),
            ActivityItem(R.string.sensor_test, SensorTest::class.java),
            ActivityItem(R.string.crash_test, CrashTest::class.java),
            ActivityItem(R.string.m_alloc_test, MAllocTest::class.java),
            ActivityItem(R.string.tts_test, TTS::class.java),
            ActivityItem(R.string.drawing_board_test, DrawingBoardTest::class.java)
        )

        recyclerView.adapter = AppMenuAdapter(this, list)
    }
}