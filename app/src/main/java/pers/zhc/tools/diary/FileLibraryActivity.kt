package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.diary_attachment_file_library_activity.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_preview_view.view.*
import pers.zhc.tools.R
import java.io.Serializable
import java.util.*
import kotlin.NoSuchElementException

/**
 * @author bczhc
 */
class FileLibraryActivity : DiaryBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_activity)
        val ll = ll!!

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
                    resultIntent.putExtra("fileInfo",
                        FileInfo(filename, addTimestamp, storageType, description, identifier))
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

    enum class StorageType(val enumInt: Int, @StringRes val textResInt: Int) {
        RAW(0, R.string.raw),
        TEXT(1, R.string.text),
        IMAGE(2, R.string.image),
        AUDIO(3, R.string.audio);

        companion object {
            operator fun get(enumInt: Int): StorageType {
                val values = values()
                values.forEach {
                    if (it.enumInt == enumInt) return it
                }
                throw NoSuchElementException()
            }
        }
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
                ctx.getString(R.string.storage_type_is, ctx.getString(StorageType.get(storageTypeEnumInt).textResInt))
            val descriptionTV = inflate.description_tv!!
            descriptionTV.text = description
            if (description.isNotEmpty()) {
                val layoutParams = descriptionTV.layoutParams
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                descriptionTV.layoutParams = layoutParams
            }
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