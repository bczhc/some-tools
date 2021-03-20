package pers.zhc.tools.diary

import android.content.Context
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
import java.io.Serializable
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

        val intent = intent
        val isPickingMode = intent.getBooleanExtra("pick", false)

        val statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment_file")
        val filenameIndex = statement.getIndexByColumnName("filename")
        val addTimestampIndex = statement.getIndexByColumnName("add_timestamp")
        val descriptionIndex = statement.getIndexByColumnName("description")
        val storageTypeIndex = statement.getIndexByColumnName("storage_type")
        val identifierIndex = statement.getIndexByColumnName("identifier")

        val cursor = statement.cursor
        while (cursor.step()) {
            val filename = cursor.getText(filenameIndex)
            val addTimestamp = cursor.getLong(addTimestampIndex)
            val description = cursor.getText(descriptionIndex)
            val storageType = cursor.getInt(storageTypeIndex)
            val identifier = cursor.getText(identifierIndex)

            val filePreviewView = getFilePreviewView(filename, addTimestamp, storageType, description)
            filePreviewView.setOnClickListener {
                if (isPickingMode) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("fileInfo", FileInfo(filename, addTimestamp, storageType, description, identifier))
                    setResult(0, resultIntent)
                    finish()
                }
            }
            ll.addView(filePreviewView)
        }
        statement.release()
    }

    fun getFilePreviewView(filename: String, addTimestamp: Long, storageTypeEnumInt: Int, description: String): View {
        return getFilePreviewView(this, filename, addTimestamp, storageTypeEnumInt, description)
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

    class FileInfo(
        val filename: String,
        val addTimestamp: Long,
        val storageTypeEnumInt: Int,
        val description: String,
        val identifier: String,
    ) : Serializable

    companion object {
        fun getFilePreviewView(
            ctx: Context,
            filename: String,
            addTimestamp: Long,
            storageTypeEnumInt: Int,
            description: String,
        ): View {
            val inflate = View.inflate(ctx, R.layout.diary_attachment_file_library_file_preview_view, null)!!
            inflate.filename_tv.text = ctx.getString(R.string.filename_is, filename)
            inflate.add_time_tv.text = ctx.getString(R.string.add_time_is, Date(addTimestamp).toString())
            inflate.storage_type_tv.text =
                ctx.getString(R.string.storage_type_is, ctx.getString(when (storageTypeEnumInt) {
                    StorageType.TEXT.enumInt -> R.string.text
                    else -> R.string.raw
                }))
            inflate.description_tv.text = description
            return inflate
        }

        fun getFilePreviewView(ctx: Context, fileInfo: FileInfo): View {
            return getFilePreviewView(ctx,
                fileInfo.filename,
                fileInfo.addTimestamp,
                fileInfo.storageTypeEnumInt,
                fileInfo.description)
        }
    }
}