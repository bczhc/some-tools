package pers.zhc.tools.diary.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.diary_attachment_fragment.*
import kotlinx.android.synthetic.main.diary_attachment_preview_view.view.*
import kotlinx.android.synthetic.main.diary_main_diary_fragment.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.diary.*
import pers.zhc.tools.diary.fragments.AttachmentFragment.Companion.EXTRA_PICKED_ATTACHMENT_ID
import pers.zhc.tools.diary.fragments.AttachmentFragment.Companion.EXTRA_PICK_MODE
import pers.zhc.tools.utils.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.jni.sqlite.Statement
import java.util.*

/**
 * @author bczhc
 */
class AttachmentFragment(
    /**
     * Be `true` if the activity is started from [DiaryContentPreviewActivity] or [DiaryTakingActivity].
     * When is `true`, the `add` menu action button will start [DiaryAttachmentActivity] with extras: [EXTRA_PICK_MODE] = true.
     */
    private var fromDiary: Boolean,

    /**
     * When is true, the list item on click action will close the current activity with the extra [EXTRA_PICKED_ATTACHMENT_ID].
     */
    private var pickMode: Boolean,

    /**
     * -1 if no dateInt specified
     */
    private var dateInt: Int = -1
) : DiaryBaseFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var itemAdapter: MyAdapter
    private val itemDataList = ArrayList<ItemData>()

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflate = inflater.inflate(R.layout.diary_attachment_fragment, container, false)

        recyclerView = inflate.recycler_view!!

        val toolbar = inflate.toolbar!!
        toolbar.setOnMenuItemClickListener(this)
        setupOuterToolbar(toolbar)

        checkAttachmentInfoRecord()
        refreshItemDataList()
        loadRecyclerView()

        return inflate
    }

    private fun loadRecyclerView() {
        itemAdapter = MyAdapter(requireContext(), itemDataList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = itemAdapter

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.thumb)!!)
        }.build()

        itemAdapter.setOnItemClickListener { position, _ ->
            val id = itemDataList[position].id
            if (pickMode) {
                activity?.apply {
                    val intent = Intent()
                    intent.putExtra(DiaryAttachmentActivity.EXTRA_PICKED_ATTACHMENT_ID, id)
                    setResult(0, intent)
                    finish()
                }
            } else {
                val intent = Intent(context, DiaryAttachmentPreviewActivity::class.java)
                intent.putExtra(DiaryAttachmentPreviewActivity.EXTRA_ATTACHMENT_ID, id)
                startActivity(intent)
            }
        }

        itemAdapter.setOnItemLongClickListener { position, view ->
            val itemData = itemDataList[position]
            val id = itemData.id

            val popupMenu =
                PopupMenuUtil.createPopupMenu(requireContext(), view, R.menu.deletion_popup_menu)
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete_btn -> {
                        DialogUtil.createConfirmationAlertDialog(context, { _, _ ->
                            // TODO check for the existence of diary attachment in diary table... (???)
                            if (fromDiary) {
                                // delete from diary attached attachment records
                                Common.doAssertion(dateInt != -1)
                                deleteAttachedAttachment(diaryDatabase, dateInt, id)
                            } else {
                                // delete from the attachment library
                                deleteAttachment(diaryDatabase, id)
                            }
                            itemDataList.removeAt(position)
                            itemAdapter.notifyItemRemoved(position)
                        }, R.string.whether_to_delete).show()
                    }
                    else -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun refreshItemDataList() {
        val statement: Statement
        if (dateInt == -1) {
            statement = diaryDatabase.compileStatement(
                """SELECT id, title, description
FROM diary_attachment"""
            )
        } else {
            statement =
                diaryDatabase.compileStatement(
                    """SELECT id, title, description
FROM diary_attachment
         INNER JOIN diary_attachment_mapping ON diary_attachment.id IS diary_attachment_mapping.referred_attachment_id
WHERE diary_attachment_mapping.diary_date IS ?"""
                )
            statement.bind(1, dateInt)
        }

        val cursor = statement.cursor
        while (cursor.step()) {
            val id = cursor.getLong(0)
            val title = cursor.getText(1)
            val description = cursor.getText(2)

            itemDataList.add(ItemData(title, description, id))
        }
        statement.release()
    }

    private fun checkAttachmentInfoRecord() {
        val fileStoragePath = DiaryAttachmentSettingsActivity.getFileStoragePath(diaryDatabase)
        if (fileStoragePath == null) {
            // record "info_json" doesn't exists, then start to set it
            startActivity(Intent(context, DiaryAttachmentSettingsActivity::class.java))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                if (fromDiary) {
                    val intent = Intent(context, DiaryAttachmentActivity::class.java)
                    intent.putExtra(EXTRA_PICK_MODE, true)
                    startActivityForResult(intent, BaseActivity.RequestCode.START_ACTIVITY_0)
                } else {
                    val intent = Intent(context, DiaryAttachmentAddingActivity::class.java)
                    startActivityForResult(intent, BaseActivity.RequestCode.START_ACTIVITY_1)
                }
            }
            R.id.file_library -> {
                startActivity(Intent(context, FileLibraryActivity::class.java))
            }
            R.id.setting_btn -> {
                startActivity(Intent(context, DiaryAttachmentSettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BaseActivity.RequestCode.START_ACTIVITY_0 -> {
                // on `add` menu action button activity returned when `fromDiary` = true (select an attachment from the attachment library)

                // no attachment picked
                data ?: return

                // `dateInt` indicates the specified diary to be attached with attachments
                Common.doAssertion(dateInt != -1)
                Common.doAssertion(data.hasExtra(EXTRA_PICKED_ATTACHMENT_ID))

                val pickedAttachmentId = data.getLongExtra(EXTRA_PICKED_ATTACHMENT_ID, -1)
                if (checkExistence(pickedAttachmentId)) {
                    ToastUtils.show(context, R.string.diary_attachment_adding_duplicate_toast)
                    return
                }
                attachAttachment(pickedAttachmentId)

                itemDataList.add(queryAttachment(pickedAttachmentId))
                itemAdapter.notifyItemChanged(itemDataList.size - 1)
            }
            BaseActivity.RequestCode.START_ACTIVITY_1 -> {
                // on attachment adding activity returned
                // update view
                data ?: return

                Common.doAssertion(data.hasExtra(DiaryAttachmentAddingActivity.EXTRA_RESULT_ATTACHMENT_ID))
                // id of the attachment just added
                val attachmentId = data.getLongExtra(DiaryAttachmentAddingActivity.EXTRA_RESULT_ATTACHMENT_ID, -1)

                itemDataList.add(queryAttachment(attachmentId))
                itemAdapter.notifyItemChanged(itemDataList.size - 1)
            }
            else -> {
            }
        }
    }

    private fun checkExistence(attachmentId: Long): Boolean {
        Common.doAssertion(dateInt != -1)
        return diaryDatabase.hasRecord(
            """SELECT *
FROM diary_attachment_mapping
WHERE diary_date IS ?
  AND referred_attachment_id IS ?""",
            arrayOf(dateInt, attachmentId)
        )
    }

    private fun queryAttachment(id: Long): ItemData {
        val statement = diaryDatabase.compileStatement(
            """SELECT *
FROM diary_attachment
WHERE id IS ?"""
        )
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
            diaryDatabase.compileStatement(
                """INSERT INTO diary_attachment_mapping(diary_date, referred_attachment_id)
VALUES (?, ?)"""
            )
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
                db.compileStatement(
                    """DELETE
FROM diary_attachment_file_reference
WHERE attachment_id IS ?"""
                )
            statement.bind(1, attachmentId)
            statement.step()
            statement.release()

            statement = db.compileStatement(
                """DELETE
FROM diary_attachment
WHERE id IS ?"""
            )
            statement.bind(1, attachmentId)
            statement.step()
            statement.release()
        }

        fun deleteAttachedAttachment(db: SQLite3, diaryDateInt: Int, attachmentId: Long) {
            db.execBind(
                """DELETE
FROM diary_attachment_mapping
WHERE diary_date IS ?
  AND referred_attachment_id IS ?""",
                arrayOf(diaryDateInt, attachmentId)
            )
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