package pers.zhc.tools.test

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CrashTestActivityBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.signals.SignalTest
import pers.zhc.tools.utils.RecyclerViewUtils
import pers.zhc.tools.utils.setLinearLayoutManager

/**
 * @author bczhc
 */
class CrashTest : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = CrashTestActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val recyclerView = bindings.recyclerView

        val menu = resources.getStringArray(R.array.crash_test_menu)
        val listAdapter = RecyclerViewUtils.buildSimpleItem1ListAdapter(this, menu.toList(), true)
        recyclerView.apply {
            adapter = listAdapter
            setLinearLayoutManager()
        }
        listAdapter.setOnItemClickListener { position, _ ->
            listOf(
                {
                    // RuntimeException
                    throw RuntimeException("Boom!")
                },
                {
                    // JNI Throw
                    JNI.CrashTest.throwException()
                },
                {
                    // Rust Panic
                    JNI.CrashTest.panic()
                },
                {
                    // Signals
                    startActivity(Intent(this, SignalTest::class.java))
                }
            )[position]()
        }
    }
}
