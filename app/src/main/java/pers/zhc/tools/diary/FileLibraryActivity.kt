package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import kotlinx.android.synthetic.main.diary_attachment_file_library_activity.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_preview_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.PopupMenuUtil
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.sqlite.SQLite3
import java.io.File
import java.lang.RuntimeException
import java.util.*

/**
 * @author bczhc
 */
class FileLibraryActivity : DiaryBaseActivity() {
    private lateinit var linearLayout: LinearLayout
    private var isPickingMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_activity)
        linearLayout = ll!!

        val intent = intent
        isPickingMode = intent.getBooleanExtra("pick", false)

        val statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment_file")
        val filenameIndex = statement.getIndexByColumnName("content")
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

            val fileInfo = FileInfo(filename, additionTimestamp, storageType, description, identifier)
            val filePreviewView = getFilePreviewView(this, fileInfo)
            setPreviewViewListener(filePreviewView, fileInfo)
            ll.addView(filePreviewView)
        }
        statement.release()
    }

    fun getFilePreviewView(
        filename: String,
        additionTimestamp: Long,
        storageTypeEnumInt: Int,
        description: String,
        identifier: String,
    ): LinearLayoutWithFileInfo {
        return getFilePreviewView(
            this,
            FileInfo(filename, additionTimestamp, storageTypeEnumInt, description, identifier)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_file_library_actionbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                startActivityForResult(
                    Intent(this, FileLibraryAddingActivity::class.java),
                    RequestCode.START_ACTIVITY_0
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun getFilePreviewView(ctx: Context, fileInfo: FileInfo): LinearLayoutWithFileInfo {
            val inflate = View.inflate(ctx, R.layout.diary_attachment_file_library_file_preview_view, null)!!
                .findViewById<LinearLayoutWithFileInfo>(R.id.ll)
            if (fileInfo.storageTypeEnumInt == StorageType.TEXT.enumInt) {
                inflate.filename_tv.visibility = View.GONE
            } else {
                inflate.filename_tv.text = ctx.getString(R.string.filename_is, fileInfo.content)
            }
            inflate.add_time_tv.text =
                ctx.getString(R.string.addition_time_is, Date(fileInfo.additionTimestamp).toString())
            inflate.storage_type_tv.text =
                ctx.getString(
                    R.string.storage_type_is,
                    ctx.getString(StorageType.get(fileInfo.storageTypeEnumInt).textResInt)
                )
            val descriptionTV = inflate.description_tv!!
            descriptionTV.text = fileInfo.description
            if (fileInfo.description.isNotEmpty()) {
                val layoutParams = descriptionTV.layoutParams
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                descriptionTV.layoutParams = layoutParams
            }
            inflate.fileInfo = fileInfo
            return inflate
        }

        fun getFileInfo(diaryDatabase: SQLite3, identifier: String): FileInfo {
            val statement =
                diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment_file\nWHERE identifier IS ?")
            statement.bindText(1, identifier)
            val contentIndex = statement.getIndexByColumnName("content")
            val additionTimestampIndex = statement.getIndexByColumnName("addition_timestamp")
            val descriptionIndex = statement.getIndexByColumnName("description")
            val storageTypeIndex = statement.getIndexByColumnName("storage_type")

            val cursor = statement.cursor
            if (!cursor.step()) {
                throw RuntimeException("Entry not found")
            }
            val filename = cursor.getText(contentIndex)
            val additionTimestamp = cursor.getLong(additionTimestampIndex)
            val description = cursor.getText(descriptionIndex)
            val storageType = cursor.getInt(storageTypeIndex)
            statement.release()

            return FileInfo(filename, additionTimestamp, storageType, description, identifier)
        }

        fun getFilePreviewView(ctx: Context, diaryDatabase: SQLite3, identifier: String): LinearLayoutWithFileInfo {
            return getFilePreviewView(ctx, getFileInfo(diaryDatabase, identifier))
        }

        fun deleteFileRecord(db: SQLite3, identifier: String) {
            val statement =
                db.compileStatement("DELETE\nFROM diary_attachment_file\nWHERE identifier IS ?;")
            statement.bindText(1, identifier)
            statement.step()
            statement.release()
        }

        fun getFileStoredPath(db: SQLite3, identifier: String): String {
            val fileStoragePath = DiaryAttachmentSettingsActivity.getFileStoragePath(db)
            return File(fileStoragePath, identifier).path
        }

        fun showFileNotExistDialog(ctx: Context, db: SQLite3, identifier: String) {
            DialogUtil.createConfirmationAlertDialog(
                ctx,
                { _, _ ->
                    deleteFileRecord(db, identifier)
                },
                R.string.diary_file_library_file_not_exist_dialog
            ).show()
        }
    }

    fun setPreviewViewListener(view: View, fileInfo: FileInfo) {
        view.setOnClickListener {
            // check file existence if storage type is file
            if (fileInfo.storageTypeEnumInt != StorageType.TEXT.enumInt) {
                val storedFile = File(getFileStoredPath(diaryDatabase, fileInfo.identifier))
                if (!storedFile.exists()) {
                    showFileNotExistDialog(this, diaryDatabase, fileInfo.identifier)
                    return@setOnClickListener
                }
            }
            if (isPickingMode) {
                val resultIntent = Intent()
                resultIntent.putExtra("fileInfo", fileInfo)
                setResult(0, resultIntent)
                finish()
            } else {
                val intent = Intent(this, FileLibraryFileDetailActivity::class.java)
                intent.putExtra(FileLibraryFileDetailActivity.EXTRA_IDENTIFIER, fileInfo.identifier)
                this.startActivity(intent)
            }
        }

        view.setOnLongClickListener {
            val pm = PopupMenuUtil.createPopupMenu(this, it, R.menu.deletion_popup_menu)
            pm.show()

            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete_btn -> {
                        showDeleteDialog(fileInfo.identifier)
                    }
                    else -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnLongClickListener true
        }
    }

    private fun showDeleteDialog(identifier: String) {
        DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
            val statement =
                diaryDatabase.compileStatement("SELECT COUNT()\nFROM diary_attachment_file\nWHERE diary_attachment_file.identifier IS ?\n  AND diary_attachment_file.identifier IN\n      (SELECT diary_attachment_file_reference.identifier FROM diary_attachment_file_reference);")
            statement.bindText(1, identifier)
            val hasRecord = diaryDatabase.hasRecord(statement)
            statement.release()

            if (hasRecord) {
                // alert
                ToastUtils.show(this, R.string.diary_file_library_has_file_reference_alert_msg)
                return@createConfirmationAlertDialog
            } else {
                // delete
                deleteFileRecord(diaryDatabase, identifier)
            }
        }, R.string.whether_to_delete).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // on FileLibraryAddingActivity returned
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            // not submit
            data ?: return

            val resultIdentifier = data.getStringExtra(FileLibraryAddingActivity.EXTRA_RESULT_IDENTIFIER)!!
            val filePreviewView = getFilePreviewView(this, diaryDatabase, resultIdentifier)
            linearLayout.addView(filePreviewView)
        }
    }
}

class LinearLayoutWithFileInfo : LinearLayout {
    var fileInfo: FileInfo? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
}