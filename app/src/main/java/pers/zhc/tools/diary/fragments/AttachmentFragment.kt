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
import kotlinx.android.synthetic.main.diary_attachment_preview_view.view.*
import kotlinx.android.synthetic.main.diary_main_diary_fragment.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.R
import pers.zhc.tools.diary.*
import pers.zhc.tools.diary.fragments.AttachmentFragment.Companion.EXTRA_PICKED_ATTACHMENT_ID
import pers.zhc.tools.diary.fragments.AttachmentFragment.Companion.EXTRA_PICK_MODE
import pers.zhc.tools.utils.*

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
    private val itemDataList = ArrayList<Attachment>()

    private lateinit var recyclerView: RecyclerView

    private val launchers = object {
        val pickAttachment = registerForActivityResult(DiaryAttachmentActivity.PickAttachmentContract()) { result ->
            result ?: return@registerForActivityResult

            // `dateInt` indicates the specified diary to be attached with attachments
            androidAssert(dateInt != -1)
            val pickedAttachmentId = result.attachmentId

            if (checkExistence(pickedAttachmentId)) {
                ToastUtils.show(context, R.string.diary_attachment_adding_duplicate_toast)
                return@registerForActivityResult
            }
            diaryDatabase.attachAttachment(dateInt, pickedAttachmentId)

            itemDataList.add(diaryDatabase.queryAttachment(pickedAttachmentId))
            itemAdapter.notifyItemChanged(itemDataList.size - 1)
        }
        val addAttachment = registerForActivityResult(DiaryAttachmentAddingActivity.AddAttachmentContract()) { result ->
            result ?: return@registerForActivityResult
            // id of the attachment just added
            val attachmentId = result.attachmentId

            itemDataList.add(diaryDatabase.queryAttachment(attachmentId))
            itemAdapter.notifyItemChanged(itemDataList.size - 1)
        }
    }

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
                            if (fromDiary) {
                                // delete from diary attached attachment records
                                androidAssert(dateInt != -1)
                                diaryDatabase.deleteAttachmentFromDiary(dateInt, id)
                            } else {
                                // delete from the attachment library
                                if (diaryDatabase.checkIfAttachmentUsedInDiary(id)) {
                                    ToastUtils.show(requireContext(), R.string.diary_attachment_has_reference_alert_msg)
                                    return@createConfirmationAlertDialog
                                }
                                diaryDatabase.deleteAttachment(id)
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
        val attachments = diaryDatabase.queryAttachments(dateInt.takeIf { it != -1 })
        itemDataList.clear()
        itemDataList.addAll(attachments)
    }

    private fun checkAttachmentInfoRecord() {
        val fileStoragePath = diaryDatabase.queryExtraInfo().nullMap { it.diaryAttachmentFileLibraryStoragePath }
        if (fileStoragePath == null) {
            // record "info_json" doesn't exist, then start to set it
            startActivity(Intent(context, DiaryAttachmentSettingsActivity::class.java))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                if (fromDiary) {
                    launchers.pickAttachment.launch(Unit)
                } else {
                    launchers.addAttachment.launch(Unit)
                }
            }
        }
        return true
    }

    private fun checkExistence(attachmentId: Long): Boolean {
        androidAssert(dateInt != -1)
        return diaryDatabase.checkDiaryAttachmentExists(dateInt, attachmentId)
    }

    class MyAdapter(private val context: Context, private val itemDataList: List<Attachment>) :
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