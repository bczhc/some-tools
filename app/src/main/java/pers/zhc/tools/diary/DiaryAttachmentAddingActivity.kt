package pers.zhc.tools.diary

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_attachment_adding_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker

class DiaryAttachmentAddingActivity : BaseActivity() {
    private lateinit var fileListLL: LinearLayout
    lateinit var linearLayout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.diary_attachment_adding_activity)
        val titleEt = title_et!!
        fileListLL = file_list_ll!!
        val descriptionEt = description_et!!
        val createAttachmentBtn = create_attachment_btn!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            data!!
            val result = data.getStringExtra("result") ?: return
            val textView = getTextView()
            textView.text = result
            textView.background = getDrawable(R.drawable.view_stroke)
            fileListLL.addView(textView)
        }
    }

    private fun getTextView(): TextView {
        val textView = TextView(this)
        textView.textSize = 20F
        textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        return textView
    }
}