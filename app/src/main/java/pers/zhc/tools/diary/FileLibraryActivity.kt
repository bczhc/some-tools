package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContract
import pers.zhc.tools.R
import pers.zhc.tools.diary.fragments.FileLibraryFragment

/**
 * @author bczhc
 */
class FileLibraryActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_file_library_activity)

        val intent = intent
        val pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, false)

        val manager = supportFragmentManager
        val fileLibraryFragment = FileLibraryFragment(pickMode)
        manager.beginTransaction().add(R.id.container, fileLibraryFragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_file_library_actionbar, menu)
        return true
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

    class PickFileContract : ActivityResultContract<Unit, PickFileContract.Result?>() {
        class Result(
            val identifier: String
        )

        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(context, FileLibraryActivity::class.java).apply {
                putExtra(EXTRA_PICK_MODE, true)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Result? {
            intent ?: return null
            return Result(
                intent.getStringExtra(EXTRA_PICKED_FILE_IDENTIFIER)!!
            )
        }
    }
}
