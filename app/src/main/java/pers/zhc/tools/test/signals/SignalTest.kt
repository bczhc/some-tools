package pers.zhc.tools.test.signals

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.SignalTestActivityBinding
import pers.zhc.tools.utils.RecyclerViewUtils
import pers.zhc.tools.utils.setLinearLayoutManager

class SignalTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = SignalTestActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val signals = Signal.signals

        val recyclerView = bindings.recyclerView
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
