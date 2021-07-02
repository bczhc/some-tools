package pers.zhc.tools.diary

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment

/**
 * @author bczhc
 */
class FileLibraryActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = supportFragmentManager
        manager.beginTransaction().add(R.id.container, FileLibraryFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_file_library_actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return false
    }

    companion object {
        /**
         * intent boolean extra
         */
        const val EXTRA_PICK_MODE = "pickMode"

        /**
         * intent string extra
         * the result extra key when [EXTRA_PICK_MODE] ([FileLibraryFragment.pickMode]) is true
         */
        const val EXTRA_PICKED_FILE_IDENTIFIER = "pickedFileIdentifier"
    }
}