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
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryFileLibraryFilePreviewViewBinding
import pers.zhc.tools.databinding.DiaryFileLibraryFragmentBinding
import pers.zhc.tools.diary.*
import pers.zhc.tools.diary.FileLibraryActivity.Companion.EXTRA_PICKED_FILE_IDENTIFIER
import pers.zhc.tools.utils.*
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class FileLibraryFragment(
    private var pickMode: Boolean = false
) : DiaryBaseFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var recyclerViewAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView
    private var itemDataList = ArrayList<ItemData>()

    private val launchers = object {
        val addFile = registerForActivityResult(FileLibraryAddingActivity.AddFileContract()) { result ->
            result ?: return@registerForActivityResult

            val identifier = result.identifier
            val fileInfo = diaryDatabase.queryAttachmentFile(identifier)!!
            var content: String? = null
            if (fileInfo.storageType == StorageType.TEXT) {
                content = diaryDatabase.queryTextAttachment(identifier)
            }
            itemDataList.add(ItemData(fileInfo, content))
            recyclerViewAdapter.notifyItemInserted(itemDataList.size - 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = DiaryFileLibraryFragmentBinding.inflate(inflater, container, false)
        recyclerView = bindings.recyclerView

        val toolbar = bindings.toolbar
        toolbar.setOnMenuItemClickListener(this)
        setupOuterToolbar(toolbar)

        loadRecyclerView()
        return bindings.root
    }

    private fun loadRecyclerView() {
        refreshItemDataList()
        recyclerViewAdapter = MyAdapter(requireContext(), itemDataList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = recyclerViewAdapter

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.thumb)!!)
        }.build()

        recyclerViewAdapter.setOnItemClickListener { position, _ ->
            val itemData = itemDataList[position]
            val fileInfo = itemData.fileInfo

            // check file existence if storage type is file
            if (fileInfo.storageType != StorageType.TEXT) {
                val storedFile = getFileStoredPath(fileInfo.identifier)
                if (!storedFile.exists()) {
                    showFileNotExistDialog(requireContext(), fileInfo.identifier)
                    return@setOnItemClickListener
                }
            }
            if (pickMode) {
                activity?.apply {
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_PICKED_FILE_IDENTIFIER, fileInfo.identifier)
                    setResult(0, resultIntent)
                    finish()
                }
            } else {
                val intent = Intent(context, FileLibraryFileDetailActivity::class.java)
                intent.putExtra(FileLibraryFileDetailActivity.EXTRA_IDENTIFIER, fileInfo.identifier)
                startActivity(intent)
            }
        }

        recyclerViewAdapter.setOnItemLongClickListener { position, view ->
            val itemData = itemDataList[position]
            val fileInfo = itemData.fileInfo

            val pm = PopupMenuUtil.createPopupMenu(requireContext(), view, R.menu.deletion_popup_menu)
            pm.show()

            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete_btn -> {
                        showDeleteDialog(fileInfo.identifier, fileInfo.storageType, position)
                    }

                    else -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnItemLongClickListener
        }
    }

    private fun refreshItemDataList() {
        itemDataList.clear()

        val attachmentFiles = diaryDatabase.queryAttachmentFiles()
        for (fileInfo in attachmentFiles) {
            val content = if (fileInfo.storageType == StorageType.TEXT) {
                diaryDatabase.queryTextAttachment(fileInfo.identifier)
            } else {
                null
            }
            itemDataList.add(ItemData(fileInfo, content))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                launchers.addFile.launch(Unit)
            }
        }
        return true
    }

    private fun getFileStoredPath(identifier: String): File {
        return File(LocalInfo.attachmentStoragePath, identifier)
    }

    private fun showFileNotExistDialog(context: Context, identifier: String) {
        DialogUtil.createConfirmationAlertDialog(
            context,
            { _, _ ->
                diaryDatabase.deleteAttachmentFile(identifier)
            },
            R.string.diary_file_library_file_not_exist_dialog
        ).show()
    }

    companion object {
        private fun newFilePreviewView(ctx: Context): View {
            return View.inflate(ctx, R.layout.diary_file_library_file_preview_view, null)!!
        }

        fun getFilePreviewView(ctx: Context, diaryDatabase: DiaryDatabase, identifier: String): View {
            val fileInfo = diaryDatabase.queryAttachmentFile(identifier)!!
            var content: String? = null
            if (fileInfo.storageType == StorageType.TEXT) {
                content = diaryDatabase.queryTextAttachment(identifier)
            }
            val newFilePreviewView = newFilePreviewView(ctx)
            setFilePreviewView(ctx, newFilePreviewView, fileInfo, content)
            return newFilePreviewView
        }

        fun getFilePreviewView(ctx: Context, fileInfo: FileInfo, content: String?): View {
            val view = newFilePreviewView(ctx)
            setFilePreviewView(ctx, view, fileInfo, content)
            return view
        }

        fun setFilePreviewView(ctx: Context, view: View, fileInfo: FileInfo, content: String?) {
            val bindings = DiaryFileLibraryFilePreviewViewBinding.bind(view)
            if (fileInfo.storageType == StorageType.TEXT) {
                bindings.filenameTv.visibility = View.GONE
            } else {
                bindings.filenameTv.text = ctx.getString(R.string.filename_is, fileInfo.filename)
            }

            bindings.addTimeTv.text =
                ctx.getString(R.string.addition_time_is, Date(fileInfo.additionTimestamp).toString())

            bindings.storageTypeTv.text =
                ctx.getString(
                    R.string.storage_type_is,
                    ctx.getString(fileInfo.storageType.textResInt)
                )

            val descriptionTV = bindings.descriptionTv
            if (fileInfo.description.isEmpty()) {
                descriptionTV.visibility = View.GONE
            } else {
                descriptionTV.text = fileInfo.description
            }

            val contentTV = bindings.contentTv
            if (content == null) {
                contentTV.visibility = View.GONE
                bindings.content.visibility = View.GONE
            } else {
                contentTV.text = if (content.length > 10) {
                    content.substring(0..10) + "..."
                } else {
                    content
                }
            }
        }
    }

    private fun showDeleteDialog(identifier: String, storageType: StorageType, position: Int) {
        DialogUtil.createConfirmationAlertDialog(context, { _, _ ->
            if (diaryDatabase.checkIfFileUsedInAttachments(identifier)) {
                ToastUtils.show(context, R.string.diary_file_library_has_file_reference_alert_msg)
                return@createConfirmationAlertDialog
            }

            when (storageType) {
                StorageType.TEXT -> {
                    diaryDatabase.deleteTextAttachment(identifier)
                }

                else -> {
                    // file
                    diaryDatabase.deleteAttachmentFile(identifier)
                    File(LocalInfo.attachmentStoragePath, identifier).requireDelete()
                }
            }
            itemDataList.removeAt(position)
            recyclerViewAdapter.notifyItemRemoved(position)
        }, R.string.whether_to_delete).show()
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
                .inflate(R.layout.diary_file_library_file_preview_view, parent, false)!!
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
