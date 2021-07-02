package pers.zhc.tools.diary.fragments

import android.content.Context
import android.os.Bundle
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import pers.zhc.tools.diary.DiaryBaseActivity
import pers.zhc.tools.utils.sqlite.SQLite3

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
}