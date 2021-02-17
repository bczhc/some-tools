package pers.zhc.tools.inputmethod

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R

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


        class MyArrayAdapter(context: Context, val resource: Int, objects: Array<out String>) :
            ArrayAdapter<String>(context, resource, objects) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = View.inflate(this@WubiInputMethodActivity, resource, null)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)!!
                return textView
            }
        }

        val data = resources.getStringArray(R.array.wubi_settings)
        setContentView(R.layout.wubi_input_method_setting_activity)
        val listView = findViewById<ListView>(R.id.lv)
        val adapter = MyArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            onClickListeners[position].onClick(view)
        }
    }
}