package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_attachment_adding_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import java.io.File

class DiaryAttachmentAddingActivity : BaseActivity() {
    private lateinit var fileListLL: LinearLayout
    lateinit var linearLayout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_adding_activity)
        val titleEt = title_et!!
        val addFileBtn = add_file_btn!!
        fileListLL = file_list_ll!!
        val descriptionEt = description_et!!
        val createAttachmentBtn = create_attachment_btn!!

        addFileBtn.setOnClickListener {
            startActivityForResult(Intent(this, FilePicker::class.java), RequestCode.START_ACTIVITY_0)
        }
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            intent!!
            val result = intent.getStringExtra("result") ?: return
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