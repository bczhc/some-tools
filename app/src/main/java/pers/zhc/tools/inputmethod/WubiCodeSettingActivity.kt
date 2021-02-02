package pers.zhc.tools.inputmethod

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.Download
import pers.zhc.tools.utils.ToastUtils
import java.io.IOException
import java.net.URL

class WubiCodeSettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listView = ListView(this)
        setContentView(listView)
        val data = resources.getStringArray(R.array.wubi_code_settings)

        class MyArrayAdapter(context: Context, val resource: Int, objects: Array<out String>) : ArrayAdapter<String>(context, resource, objects) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = View.inflate(this@WubiCodeSettingActivity, resource, null)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)!!
                return textView
            }
        }

        listView.adapter = MyArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            arrayOf(
                    View.OnClickListener {
                        try {
                            val wubiDatabaseURL = URL(Common.getGithubRawFileURLString("bczhc", "master", "wubi_code.db"))
                            val localWubiDatabaseFile = Common.getInternalDatabaseDir(this, "wubi_code.db")
                            Download.startDownloadWithDialog(this, wubiDatabaseURL, localWubiDatabaseFile) {
                                runOnUiThread {
                                    ToastUtils.show(this, R.string.downloading_done)
                                }
                            }
                        } catch (e: IOException) {
                            Common.showException(e, this)
                        }
                    }
            )[position].onClick(view)
        }
    }
}