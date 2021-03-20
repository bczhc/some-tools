package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.diary_attachment_file_library_activity.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_preview_view.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.sqlite.SQLite3
import java.util.*

/**
 * @author bczhc
 */
class FileLibraryActivity : BaseActivity() {
    private lateinit var diaryDatabase: SQLite3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_activity)
        val ll = ll!!
        diaryDatabase = DiaryMainActivity.getDiaryDatabase(this)

        val statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment_file")
        val filenameIndex = statement.getIndexByColumnName("filename")
        val addTimestampIndex = statement.getIndexByColumnName("add_timestamp")
        val descriptionIndex = statement.getIndexByColumnName("description")
        val storageTypeIndex = statement.getIndexByColumnName("storage_type")

        val cursor = statement.cursor
        while (cursor.step()) {
            val filename = cursor.getText(filenameIndex)
            val addTimestamp = cursor.getLong(addTimestampIndex)
            val description = cursor.getText(descriptionIndex)
            val storageType = cursor.getInt(storageTypeIndex)

            val filePreviewView = getFilePreviewView(filename, addTimestamp, storageType, description)
            ll.addView(filePreviewView)
        }
        statement.release()
    }

    fun getFilePreviewView(filename: String, addTimestamp: Long, storageTypeEnumInt: Int, description: String): View {
        val inflate = View.inflate(this, R.layout.diary_attachment_file_library_file_preview_view, null)!!
        inflate.filename_tv.text = getString(R.string.filename_is, filename)
        inflate.add_time_tv.text = getString(R.string.add_time_is, Date(addTimestamp).toString())
        inflate.storage_type_tv.text = getString(R.string.storage_type_is, getString(when (storageTypeEnumInt) {
            StorageType.TEXT.enumInt -> R.string.text
            else -> R.string.raw
        }))
        inflate.description_tv.text = getString(R.string.description_is, description)
        return inflate
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_file_library_actionbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, FileLibraryAddingActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    enum class StorageType(ordinal: Int) {
        RAW(0),
        TEXT(1);

        val enumInt: Int = ordinal
    }
}