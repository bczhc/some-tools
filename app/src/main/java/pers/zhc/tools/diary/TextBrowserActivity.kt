package pers.zhc.tools.diary

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ScrollView
import android.widget.TextView
import pers.zhc.tools.diary.fragments.FileLibraryFragment

/**
 * @author bczhc
 */
class TextBrowserActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        if (!intent.hasExtra(EXTRA_IDENTIFIER)) {
            return
        }
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER)!!
        val content = diaryDatabase.queryTextAttachment(identifier)

        setContentView(ScrollView(this).apply {
            addView(TextView(this@TextBrowserActivity).apply {
                text = content
                textSize = DiaryContentPreviewActivity.getEditTextTextSize(this@TextBrowserActivity)
                setTextIsSelectable(true)
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            })
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        })
    }

    companion object {
        const val EXTRA_IDENTIFIER = "identifier"
    }
}