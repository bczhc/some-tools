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
        val additionTimestampIndex = statement.getIndexByColumnName("addition_timestamp")
        val descriptionIndex = statement.getIndexByColumnName("description")
        val storageTypeIndex = statement.getIndexByColumnName("storage_type")
        val identifierIndex = statement.getIndexByColumnName("identifier")

        val cursor = statement.cursor
        while (cursor.step()) {
            val filename = cursor.getText(filenameIndex)
            val additionTimestamp = cursor.getLong(additionTimestampIndex)
            val description = cursor.getText(descriptionIndex)
            val storageType = cursor.getInt(storageTypeIndex)
            val identifier = cursor.getText(identifierIndex)

            val filePreviewView = getFilePreviewView(filename, additionTimestamp, storageType, description, identifier)
            filePreviewView.setOnClickListener {
                if (isPickingMode) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("fileInfo",
                        FileInfo(filename, additionTimestamp, storageType, description, identifier))
                    setResult(0, resultIntent)
                    finish()
                }
            }
            ll.addView(filePreviewView)
        }
        statement.release()
    }

    fun getFilePreviewView(
        filename: String,
        additionTimestamp: Long,
        storageTypeEnumInt: Int,
        description: String,
        identifier: String
    ): View {
        return getFilePreviewView(this, FileInfo(filename, additionTimestamp, storageTypeEnumInt, description, identifier))
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
            fun get(enumInt: Int): StorageType {
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
        val additionTimestamp: Long,
        val storageTypeEnumInt: Int,
        val description: String,
        val identifier: String,
    ) : Serializable

    companion object {
        fun getFilePreviewView(ctx: Context, fileInfo: FileInfo): View {
            val inflate = View.inflate(ctx, R.layout.diary_attachment_file_library_file_preview_view, null)!!
            inflate.filename_tv.text = ctx.getString(R.string.filename_is, fileInfo.filename)
            inflate.add_time_tv.text = ctx.getString(R.string.addition_time_is, Date(fileInfo.additionTimestamp).toString())
            inflate.storage_type_tv.text =
                ctx.getString(R.string.storage_type_is, ctx.getString(StorageType.get(fileInfo.storageTypeEnumInt).textResInt))
            val descriptionTV = inflate.description_tv!!
            descriptionTV.text = fileInfo.description
            if (fileInfo.description.isNotEmpty()) {
                val layoutParams = descriptionTV.layoutParams
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                descriptionTV.layoutParams = layoutParams
            }

            inflate.setOnClickListener {
                val intent = Intent(ctx, FileLibraryFileDetailActivity::class.java)
                intent.putExtra("fileInfo", fileInfo)
                ctx.startActivity(intent)
            }
            return inflate
        }
    }
}