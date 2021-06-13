package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.diary_attachment_file_library_activity.*
import kotlinx.android.synthetic.main.diary_attachment_file_library_file_preview_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.sqlite.SQLite3
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class FileLibraryActivity : DiaryBaseActivity() {
    private lateinit var recyclerViewAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView
    private var itemDataList = ArrayList<ItemData>()
    private var pickMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_file_library_activity)

        recyclerView = recycler_view!!

        val intent = intent
        pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, false)

        showList()
    }

    private fun showList() {
        refreshItemDataList()
        recyclerViewAdapter = MyAdapter(this, itemDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recyclerViewAdapter

        recyclerViewAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onClick(position: Int, view: View) {
                val itemData = itemDataList[position]
                val fileInfo = itemData.fileInfo

                // check file existence if storage type is file
                if (fileInfo.storageTypeEnumInt != StorageType.TEXT.enumInt) {
                    val storedFile = File(getFileStoredPath(diaryDatabase, fileInfo.identifier))
                    if (!storedFile.exists()) {
                        showFileNotExistDialog(this@FileLibraryActivity, diaryDatabase, fileInfo.identifier)
                        return
                    }
                }
                if (pickMode) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_PICKED_FILE_IDENTIFIER, fileInfo.identifier)
                    setResult(0, resultIntent)
                    finish()
                } else {
                    val intent = Intent(this@FileLibraryActivity, FileLibraryFileDetailActivity::class.java)
                    intent.putExtra(FileLibraryFileDetailActivity.EXTRA_IDENTIFIER, fileInfo.identifier)
                    startActivity(intent)
                }
            }
        })

        recyclerViewAdapter.setOnItemLongClickListener(object : OnItemLongClickListener {
            override fun onLongClick(position: Int, view: View) {
                val itemData = itemDataList[position]
                val fileInfo = itemData.fileInfo

                val pm = PopupMenuUtil.createPopupMenu(this@FileLibraryActivity, view, R.menu.deletion_popup_menu)
                pm.show()

                pm.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.delete_btn -> {
                            showDeleteDialog(fileInfo.identifier, fileInfo.storageTypeEnumInt, position)
                        }
                        else -> {
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                return
            }
        })
    }

    private fun refreshItemDataList() {
        itemDataList.clear()
        val statement =
            diaryDatabase.compileStatement("SELECT filename, addition_timestamp, description, storage_type, identifier\nFROM diary_attachment_file")

        val cursor = statement.cursor
        while (cursor.step()) {
            val filename = cursor.getText(0)
            val additionTimestamp = cursor.getLong(1)
            val description = cursor.getText(2)
            val storageType = cursor.getInt(3)
            val identifier = cursor.getText(4)

            var content: String? = null
            if (storageType == StorageType.TEXT.enumInt) {
                val statement1 =
                    diaryDatabase.compileStatement("SELECT content\nFROM diary_attachment_text\nWHERE identifier IS ?")
                statement1.bindText(1, identifier)
                val cursor1 = statement1.cursor
                // all text attachments are stored in the database
                Common.doAssertion(cursor1.step())
                content = cursor1.getText(0)
                statement1.release()
            }

            val fileInfo = FileInfo(filename, additionTimestamp, storageType, description, identifier)
            itemDataList.add(ItemData(fileInfo, content))
        }
        statement.release()
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
        private fun newFilePreviewView(ctx: Context): View {
            return View.inflate(ctx, R.layout.diary_attachment_file_library_file_preview_view, null)!!
        }

        fun getFilePreviewView(ctx: Context, fileInfo: FileInfo, content: String?): View {
            val view = newFilePreviewView(ctx)
            setFilePreviewView(ctx, view, fileInfo, content)
            return view
        }

        fun getFilePreviewView(ctx: Context, diaryDatabase: SQLite3, identifier: String): View {
            val fileInfo = getFileInfo(diaryDatabase, identifier)
            var content: String? = null
            if (fileInfo.storageTypeEnumInt == StorageType.TEXT.enumInt) {
                content = getTextContent(diaryDatabase, identifier)
            }
            val newFilePreviewView = newFilePreviewView(ctx)
            setFilePreviewView(ctx, newFilePreviewView, fileInfo, content)
            return newFilePreviewView
        }

        fun setFilePreviewView(ctx: Context, view: View, fileInfo: FileInfo, content: String?) {
            if (fileInfo.storageTypeEnumInt == StorageType.TEXT.enumInt) {
                view.filename_tv.visibility = View.GONE
            } else {
                view.filename_tv.text = ctx.getString(R.string.filename_is, fileInfo.filename)
            }

            view.add_time_tv.text =
                ctx.getString(R.string.addition_time_is, Date(fileInfo.additionTimestamp).toString())

            view.storage_type_tv.text =
                ctx.getString(
                    R.string.storage_type_is,
                    ctx.getString(StorageType.get(fileInfo.storageTypeEnumInt).textResInt)
                )

            val descriptionTV = view.description_tv!!
            if (fileInfo.description.isEmpty()) {
                view.description.visibility = View.GONE
                descriptionTV.visibility = View.GONE
            } else {
                descriptionTV.text = fileInfo.description
            }

            val contentTV = view.content_tv!!
            if (content == null) {
                contentTV.visibility = View.GONE
                view.content.visibility = View.GONE
            } else {
                contentTV.text = if (content.length > 10) {
                    content.substring(0..10) + "..."
                } else {
                    content
                }
            }
        }

        fun getFileInfo(diaryDatabase: SQLite3, identifier: String): FileInfo {
            val statement =
                diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment_file\nWHERE identifier IS ?")
            statement.bindText(1, identifier)
            val contentIndex = statement.getIndexByColumnName("filename")
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

        fun getTextContent(db: SQLite3, identifier: String): String {
            val statement =
                db.compileStatement("SELECT content\nFROM diary_attachment_text\nWHERE identifier IS ?")
            statement.bindText(1, identifier)
            val cursor = statement.cursor
            Common.doAssertion(cursor.step())
            val content = cursor.getText(0)!!
            statement.release()
            return content
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

        /**
         * intent boolean extra
         */
        const val EXTRA_PICK_MODE = "pickMode"

        /**
         * intent string extra
         * the result extra key when [EXTRA_PICK_MODE] ([pickMode]) is true
         */
        const val EXTRA_PICKED_FILE_IDENTIFIER = "pickedFileIdentifier"
    }

    private fun showDeleteDialog(identifier: String, storageType: Int, position: Int) {
        DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
            val hasRecord = diaryDatabase.hasRecord(
                "SELECT *\nFROM diary_attachment_file\nWHERE diary_attachment_file.identifier IS ?\n  AND diary_attachment_file.identifier IN\n      (SELECT diary_attachment_file_reference.identifier FROM diary_attachment_file_reference);",
                arrayOf(identifier)
            )

            itemDataList.removeAt(position)
            recyclerViewAdapter.notifyItemRemoved(position)

            if (hasRecord) {
                // alert
                ToastUtils.show(this, R.string.diary_file_library_has_file_reference_alert_msg)
                return@createConfirmationAlertDialog
            } else {
                // delete
                if (storageType == StorageType.TEXT.enumInt) {
                    deleteTextRecord(identifier)
                }
                deleteFileRecord(diaryDatabase, identifier)
            }
        }, R.string.whether_to_delete).show()
    }

    private fun deleteTextRecord(identifier: String) {
        diaryDatabase.execBind("DELETE\nFROM diary_attachment_text\nWHERE identifier IS ?", arrayOf(identifier))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // on FileLibraryAddingActivity returned
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            // not submit
            data ?: return

            val identifier = data.getStringExtra(FileLibraryAddingActivity.EXTRA_RESULT_IDENTIFIER)!!
            val fileInfo = getFileInfo(diaryDatabase, identifier)
            var content: String? = null
            if (fileInfo.storageTypeEnumInt == StorageType.TEXT.enumInt) {
                content = getTextContent(diaryDatabase, identifier)
            }
            itemDataList.add(ItemData(fileInfo, content))
            recyclerViewAdapter.notifyItemInserted(itemDataList.size - 1)
        }
    }

    class ItemData(
        val fileInfo: FileInfo,
        /**
         * for text attachment
         */
        val content: String?
    )

    class MyAdapter(
        private val ctx: Context,
        private val itemDataList: ArrayList<ItemData>
    ) : AdapterWithClickListener<MyAdapter.MyViewHolder>() {
        class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        private fun createView(parent: ViewGroup): View {
            return LayoutInflater.from(ctx)
                .inflate(R.layout.diary_attachment_file_library_file_preview_view, parent, false)!!
        }

        private fun bindView(view: View, itemData: ItemData) {
            setFilePreviewView(ctx, view, itemData.fileInfo, itemData.content)
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val view = createView(parent)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            bindView(holder.itemView, itemDataList[position])
        }

        override fun getItemCount(): Int {
            return itemDataList.size
        }
    }
}