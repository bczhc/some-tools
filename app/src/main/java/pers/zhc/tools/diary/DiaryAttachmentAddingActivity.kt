package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.Common
import java.io.File

class DiaryAttachmentAddingActivity : BaseActivity() {
    lateinit var linearLayout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_adding_activity)
        linearLayout = findViewById(R.id.ll)!!
        val pickFileBtn = findViewById<Button>(R.id.pick_file_btn)!!
        val saveBtn = findViewById<Button>(R.id.save_btn)!!

        pickFileBtn.setOnClickListener {
            startActivityForResult(Intent(this, FilePicker::class.java), RequestCode.START_ACTIVITY_0)
        }

        saveBtn.setOnClickListener {
            val diaryAttachmentDir = File(filesDir, "diary_attachment")

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            val result = data?.getStringExtra("result") ?: return
            val textView = getTextView()
            textView.text = result
            linearLayout.addView(textView)
        }
    }

    private fun getTextView(): TextView {
        val textView = TextView(this)
        textView.textSize = 20F
        textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        return textView
    }
}