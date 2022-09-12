package pers.zhc.tools.test.signals

import android.os.Bundle
import kotlinx.android.synthetic.main.signal_test_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.RecyclerViewUtils
import pers.zhc.tools.utils.setLinearLayoutManager

class SignalTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signal_test_activity)

        val signals = Signal.signals

        val recyclerView = recycler_view!!
        val adapter = RecyclerViewUtils.buildSimpleItem1ListAdapter(
            this, signals.map { "${it.name} ${it.int}" }, true
        )
        recyclerView.adapter = adapter
        recyclerView.setLinearLayoutManager()
        adapter.setOnItemClickListener { position, _ ->
            signals[position].raise()
        }
    }
}