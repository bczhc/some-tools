package pers.zhc.tools.wubi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.wubi_input_method_setting_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.RecyclerViewUtils
import pers.zhc.tools.utils.addDividerLines
import pers.zhc.tools.utils.setLinearLayoutManager

class WubiInputMethodActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ready()
    }

    private fun ready() {
        val onClickListeners = arrayOf(
            View.OnClickListener {
                startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
            },
            View.OnClickListener {
                (applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            },
            View.OnClickListener {
                startActivity(Intent(this, WubiCodeSettingActivity::class.java))
            },
            View.OnClickListener {
                startActivity(Intent(this, WubiInputMethodTTSSettingActivity::class.java))
            }
        )

        val data = resources.getStringArray(R.array.wubi_settings)
        setContentView(R.layout.wubi_input_method_setting_activity)

        val recyclerView = recycler_view!!
        val adapter = RecyclerViewUtils.buildSimpleItem1ListAdapter(
            this, data.toList()
        )
        adapter.setOnItemClickListener { position, view ->
            onClickListeners[position].onClick(view)
        }
        recyclerView.adapter = adapter
        recyclerView.setLinearLayoutManager()
        recyclerView.addDividerLines()
    }
}