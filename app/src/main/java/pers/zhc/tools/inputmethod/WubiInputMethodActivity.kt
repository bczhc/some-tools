package pers.zhc.tools.inputmethod

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

class WubiInputMethodActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val checkDownloadDialog = Dialog(this)
        checkDownloadDialog.setCancelable(false)
        checkDownloadDialog.setCanceledOnTouchOutside(false)
        val view = TextView(this)
        view.setText(R.string.checking_local_needed_files)
        checkDownloadDialog.setContentView(view)
        checkDownloadDialog.show()
        try {
            val wubiDatabaseURL = URL(Common.getGithubRawFileURLString("bczhc", "master", "wubi_code.db"))
            val md5TextFileURL = URL(Common.getGithubRawFileURLString("bczhc", "master", "wubi_code.db.md5"))
            val localWubiDatabaseFile = Common.getInternalDatabaseDir(this, "wubi_code.db")
            Download.checkMD5(md5TextFileURL, localWubiDatabaseFile) { result: Boolean ->
                checkDownloadDialog.dismiss()
                if (result) runOnUiThread { ready() } else runOnUiThread {
                    Download.startDownloadWithDialog(this, wubiDatabaseURL, localWubiDatabaseFile) {
                        runOnUiThread { ready() }
                    }
                }
            }
        } catch (e: IOException) {
            Common.showException(e, this)
        }
    }

    private fun ready() {
        ToastUtils.show(this, "ok")
        val onClickListeners = arrayOf(
                View.OnClickListener {
                    startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
                },
                View.OnClickListener {
                    (applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                },
                View.OnClickListener {
                    startActivity(Intent(this, WubiInputMethodTTSSettingActivity::class.java))
                }
        )


        class MyArrayAdapter(context: Context, val resource: Int, objects: Array<out String>) : ArrayAdapter<String>(context, resource, objects) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = View.inflate(this@WubiInputMethodActivity, resource, null)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)!!
                textView.setOnClickListener(onClickListeners[position])
                return textView
            }
        }

        val data = resources.getStringArray(R.array.wubi_settings)
        setContentView(R.layout.wubi_input_method_setting_activity)
        val listView = findViewById<ListView>(R.id.lv)
        val adapter = MyArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        listView.adapter = adapter
    }
}