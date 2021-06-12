package pers.zhc.tools.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.diary_attachment_activity.*
import kotlinx.android.synthetic.main.diary_attachment_preview_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.sqlite.SQLite3
import pers.zhc.tools.utils.sqlite.Statement

class DiaryAttachmentActivity : DiaryBaseActivity() {
    private lateinit var itemAdapter: MyAdapter
    private val itemDataList = ArrayList<ItemData>()

    /**
     * Be `true` if the activity is started from [DiaryContentPreviewActivity] or [DiaryTakingActivity].
     * When is `true`, the `add` menu action button will start [DiaryAttachmentActivity] with extras: [EXTRA_PICK_MODE] = true.
     */
    private var fromDiary = false

    /**
     * When is true, the list item on click action will close the current activity with the extra [EXTRA_PICKED_ATTACHMENT_ID].
     */
    private var pickMode = false

    /**
     * -1 if no dateInt specified
     */
    private var dateInt: Int = -1
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_activity)

        this.recyclerView = recycler_view!!

        val intent = intent
        if (intent.hasExtra(EXTRA_DATE_INT)) {
            dateInt = intent.getIntExtra(EXTRA_DATE_INT, -1)
        }
        fromDiary = intent.getBooleanExtra(EXTRA_FROM_DIARY, false)
        pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, false)

        checkAttachmentInfoRecord()

        refreshItemDataList()
        showViews()
    }

    private fun showViews() {
        itemAdapter = MyAdapter(this, itemDataList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = itemAdapter

        itemAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onClick(position: Int, view: View) {
                val id = itemDataList[position].id
                if (pickMode) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_PICKED_ATTACHMENT_ID, id)
                    setResult(0, resultIntent)
                    finish()
                } else {
                    val intent = Intent()
                    intent.putExtra(DiaryAttachmentPreviewActivity.EXTRA_ATTACHMENT_ID, id)
                    startActivity(intent)
                }
            }
        })

        itemAdapter.setOnItemLongClickListener(object : OnItemLongClickListener {
            override fun onLongClick(position: Int, view: View) {
                val id = itemDataList[position].id

                val popupMenu =
                    PopupMenuUtil.createPopupMenu(this@DiaryAttachmentActivity, view, R.menu.deletion_popup_menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.delete_btn -> {
                            DialogUtil.createConfirmationAlertDialog(this@DiaryAttachmentActivity, { _, _ ->
                                // TODO check for the existence of diary attachment in diary table...
                                deleteAttachment(diaryDatabase, id)
                            }, R.string.whether_to_delete).show()
                        }
                        else -> {
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }
        })
    }

    private fun refreshItemDataList() {
        val statement: Statement
        if (dateInt == -1) {
            statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment")
        } else {
            statement =
                diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment\nWHERE id IS (SELECT referred_attachment_id FROM diary_attachment_mapping WHERE diary_date IS ?);")
            statement.bind(1, dateInt)
        }
        val idColumnIndex = statement.getIndexByColumnName("id")
        val titleColumnIndex = statement.getIndexByColumnName("title")
        val descriptionColumnIndex = statement.getIndexByColumnName("description")

        val cursor = statement.cursor
        while (cursor.step()) {
            val title = cursor.getText(titleColumnIndex)
            val description = cursor.getText(descriptionColumnIndex)
            val id = cursor.getLong(idColumnIndex)

            itemDataList.add(ItemData(title, description, id))
        }
        statement.release()
    }

    private fun checkAttachmentInfoRecord() {
        val fileStoragePath = DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)
        if (fileStoragePath == null) {
            // record "info_json" doesn't exists, then start to set it
            startActivity(Intent(this, DiaryAttachmentSettingsActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_attachment_actionbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                if (fromDiary) {
                    val intent = Intent(this, this.javaClass)
                    intent.putExtra(EXTRA_PICK_MODE, true)
                    startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
                } else {
                    val intent = Intent(this, DiaryAttachmentAddingActivity::class.java)
                    startActivityForResult(intent, RequestCode.START_ACTIVITY_1)
                }
            }
            R.id.file_library -> {
                startActivity(Intent(this, FileLibraryActivity::class.java))
            }
            R.id.setting_btn -> {
                startActivity(Intent(this, DiaryAttachmentSettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // on `add` menu action button activity returned when `fromDiary` = true (select an attachment from the attachment library)

                // no attachment picked
                data ?: return

                // `dateInt` indicates the specified diary to be attached with attachments
                Common.doAssertion(dateInt != -1)
                Common.doAssertion(data.hasExtra(EXTRA_PICKED_ATTACHMENT_ID))

                val pickedAttachmentId = data.getLongExtra(EXTRA_PICKED_ATTACHMENT_ID, -1)
                attachAttachment(pickedAttachmentId)

                itemDataList.add(queryAttachment(pickedAttachmentId))
                itemAdapter.notifyItemChanged(itemDataList.size - 1)
                ToastUtils.show(this, R.string.adding_succeeded)
            }
            RequestCode.START_ACTIVITY_1 -> {
                // on attachment adding activity returned
                // update view
                data ?: return

                Common.doAssertion(data.hasExtra(DiaryAttachmentAddingActivity.EXTRA_ATTACHMENT_ID))
                // id of the attachment just added
                val attachmentId = data.getLongExtra(DiaryAttachmentAddingActivity.EXTRA_ATTACHMENT_ID, -1)

                itemDataList.add(queryAttachment(attachmentId))
                itemAdapter.notifyItemChanged(itemDataList.size - 1)
            }
            else -> {
            }
        }
    }

    private fun queryAttachment(id: Long): ItemData {
        val statement = diaryDatabase.compileStatement("SELECT *\nFROM diary_attachment\nWHERE id IS ?")
        val titleColumn = statement.getIndexByColumnName("title")
        val descriptionColumn = statement.getIndexByColumnName("description")
        statement.bind(1, id)
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())

        val title = cursor.getText(titleColumn)
        val description = cursor.getText(descriptionColumn)

        statement.release()

        return ItemData(title, description, id)
    }

    /**
     * attach an attachment to diary
     */
    private fun attachAttachment(pickedAttachmentId: Long) {
        Common.doAssertion(dateInt != -1)
        val statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_mapping(diary_date, referred_attachment_id)\nVALUES (?, ?)")
        statement.bind(1, dateInt)
        statement.bind(2, pickedAttachmentId)
        statement.step()
        statement.release()
    }

    class ItemData(val title: String, val description: String, val id: Long)

    class MyAdapter(private val context: Context, private val itemDataList: List<ItemData>) :
        AdapterWithClickListener<MyAdapter.MyViewHolder>() {
        class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        private fun createPreviewView(parent: ViewGroup): View {
            return LayoutInflater.from(context).inflate(R.layout.diary_attachment_preview_view, parent, false)
        }

        private fun bindPreviewView(viewHolder: MyViewHolder, title: String, description: String) {
            val view = viewHolder.itemView
            val titleTV = view.title_tv
            val descriptionTV = view.description_tv

            titleTV.text = context.getString(R.string.title_is, title)
            descriptionTV.text = context.getString(R.string.description_is_text, description)
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            return MyViewHolder(createPreviewView(parent))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = itemDataList[position]
            bindPreviewView(holder, item.title, item.description)
        }

        override fun getItemCount(): Int {
            return itemDataList.size
        }
    }

    companion object {
        fun deleteAttachment(db: SQLite3, attachmentId: Long) {
            var statement =
                db.compileStatement("DELETE\nFROM diary_attachment_mapping\nWHERE referred_attachment_id IS ?")
            statement.bind(1, attachmentId)
            statement.step()
            statement.release()

            statement = db.compileStatement("DELETE\nFROM diary_attachment_file_reference\nWHERE attachment_id IS ?")
            statement.bind(1, attachmentId)
            statement.step()
            statement.release()

            statement = db.compileStatement("DELETE\nFROM diary_attachment\nWHERE id IS ?")
            statement.bind(1, attachmentId)
            statement.step()
            statement.release()
        }

        /**
         * intent long extra
         * When [EXTRA_PICK_MODE] extra is `true`, this extra will be used in the result intent extras.
         */
        const val EXTRA_PICKED_ATTACHMENT_ID = "pickedAttachmentId"

        /**
         * intent integer extra
         */
        const val EXTRA_DATE_INT = "dateInt"

        /**
         * intent boolean extra
         * See [pickMode].
         */
        const val EXTRA_PICK_MODE = "pickMode"

        /**
         * intent boolean extra
         * See [fromDiary] property.
         */
        const val EXTRA_FROM_DIARY = "fromDiary"
    }

}