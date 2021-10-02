package pers.zhc.tools.diary.fragments

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import pers.zhc.tools.diary.DiaryBaseActivity
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.diary.DiaryMainActivity

/**
 * @author bczhc
 */
open class DiaryBaseFragment : Fragment() {
    /**
     * A shortcut reference to the database object of the activity which attaches the current fragment
     */
    protected lateinit var diaryDatabase: SQLite3

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val diaryBaseActivity = activity as DiaryBaseActivity
        diaryDatabase = diaryBaseActivity.diaryDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    protected fun setupOuterToolbar(toolbar: Toolbar) {
        val activity = requireActivity()
        if (activity is DiaryMainActivity) {
            activity.configureDrawerToggle(toolbar)
        }
    }
}