package pers.zhc.tools.magic

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.MagicFileListActivityBinding
import pers.zhc.tools.databinding.MagicFileListItemBinding
import pers.zhc.tools.databinding.MagicProgressDialogBinding
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.DownloadUtils
import java.io.File

/**
 * @author bczhc
 */
class FileListActivity : BaseActivity() {
    private lateinit var recyclerAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var magic: Magic
    private lateinit var magicDatabase: File
    private lateinit var progressManager: ProgressDialogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        magicDatabase = File(filesDir, "magic.mgc")
        magic = Magic()
        progressManager = ProgressDialogManager(this)

        // TODO: 7/4/21 check digest (when magic.mgc has a bad integrity, then the activity will keep crashing
        if (/*!magicDatabase.exists()*/ true/* every time download and overwrite (workaround) */) {

            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->
                downloadAndLoad()
            }, { _, _ -> finish() }, R.string.magic_missing_database_dialog).apply {
                setCanceledOnTouchOutside(false)
                setCancelable(false)
            }.show()

        } else {
            load()
        }
    }

    private fun downloadAndLoad() {
        lifecycleScope.launch {
            DownloadUtils.startDownloadWithDialog(
                this@FileListActivity, Url(Common.getStaticResourceUrlString("magic.mgc")),
                File(filesDir, "magic.mgc")
            )

            withContext(Dispatchers.Main) {
                load()
            }
        }
    }

    fun load() {
        val bindings = MagicFileListActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        recyclerView = bindings.fileListRv
        val pathTV = bindings.pathTv

        initMagic()

        recyclerAdapter = MyAdapter(this, magic)
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(this@FileListActivity, R.drawable.thumb)!!)
        }.build()

        pathTV.text = recyclerAdapter.getCurrentPath()
        recyclerAdapter.setOnPathChangedListener {
            pathTV.text = it
        }

        recyclerAdapter.asyncUpdateListWithProgress()
    }

    private fun initMagic() {
        try {
            magic.load(magicDatabase.path)
        } catch (_: RuntimeException) {
            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->

                magic.close()
                downloadAndLoad()

            }, { _, _ -> finish() }, R.string.magic_load_failed_dialog).show()
            return
        }
    }

    override fun onBackPressed() {
        if (recyclerAdapter.getCurrentPathFile().parentFile == null) {
            super.onBackPressed()
            return
        }

        recyclerAdapter.asyncPrevious()
    }

    override fun finish() {
        magic.close()
        super.finish()
    }

    enum class FileType {
        FILE,
        FOLDER,
    }

    data class FileItem(
        val fileName: String,
        val fileInfo: String,
        val fileType: FileType,
        val file: File
    )

    class MyAdapter(private val outer: FileListActivity, private val magic: Magic) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private var onPathChangedListener: OnPathChangedListener? = null
        private var currentPathFile: File
        private var fileItemList = ArrayList<FileItem>()

        init {
            currentPathFile = File(Common.getExternalStoragePath(outer))
        }

        private fun setPath(path: String) {
            currentPathFile = File(path)
            onPathChangedListener?.invoke(getCurrentPath())
        }

        enum class ProgressType {
            LIST,
            DESCRIBE,
            SORT,
            DONE
        }

        fun getCurrentPathFile(): File {
            return currentPathFile
        }

        private fun getItemList(progressCallback: ProgressCallback?): ArrayList<FileItem> {
            val result = ArrayList<FileItem>()

            val listFiles = currentPathFile.listFiles() ?: arrayOf()

            progressCallback?.invoke(ProgressType.LIST, -1, -1)
            val filesCount = listFiles.size
            progressCallback?.invoke(ProgressType.DESCRIBE, 0, filesCount)

            listFiles.forEachIndexed { index, file ->
                val info = magic.file(file.path)
                result.add(
                    FileItem(
                        file.name,
                        info,
                        if (file.isDirectory) {
                            FileType.FOLDER
                        } else {
                            FileType.FILE
                        },
                        file
                    )
                )

                progressCallback?.invoke(ProgressType.DESCRIBE, index + 1, filesCount)
            }

            progressCallback?.invoke(ProgressType.SORT, -1, -1)

            val nonHiddenFirstComparator = Comparator { f1: FileItem, f2: FileItem ->
                if (f1.file.isHidden == f2.file.isHidden) return@Comparator 0
                if (!f1.file.isHidden) return@Comparator -1
                return@Comparator 1
            }
            val fileTypeFirstComparator = Comparator { f1: FileItem, f2: FileItem ->
                if (f1.fileType == f2.fileType) return@Comparator 0
                if (f1.fileType == FileType.FOLDER) return@Comparator -1
                return@Comparator 1
            }

            // perform multiple stable sorting operations
            result.sortBy { it.fileName }
            result.sortWith(nonHiddenFirstComparator)
            result.sortWith(fileTypeFirstComparator)

            progressCallback?.invoke(ProgressType.DONE, filesCount, filesCount)
            return result
        }

        private fun asyncUpdateData(progressCallback: ProgressCallback?, done: Runnable) {
            Thread {
                val itemList = getItemList(progressCallback)
                outer.runOnUiThread {
                    fileItemList = itemList
                    done.run()
                }
            }.start()
        }

        fun asyncPrevious() {
            val parentFile = currentPathFile.parentFile!!
            currentPathFile = parentFile
            asyncUpdateListWithProgress()
        }

        fun getCurrentPath(): String {
            return currentPathFile.path
        }

        fun setOnPathChangedListener(listener: OnPathChangedListener?) {
            onPathChangedListener = listener
        }

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val bindings = MagicFileListItemBinding.bind(view)
            val filenameTV: TextView = bindings.filenameTv
            val fileInfoTV: TextView = bindings.fileInfoTv
            val fileTypeIV: ImageView = bindings.iv
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(outer).inflate(R.layout.magic_file_list_item, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val fileItem = fileItemList[position]
            holder.filenameTV.text = fileItem.fileName
            holder.fileInfoTV.text = fileItem.fileInfo
            holder.fileTypeIV.setImageResource(
                when (fileItem.fileType) {
                    FileType.FILE -> R.drawable.ic_file
                    FileType.FOLDER -> R.drawable.ic_folder
                }
            )

            holder.itemView.setOnClickListener {
                if (fileItem.fileType == FileType.FOLDER) {
                    setPath(fileItem.file.path)

                    asyncUpdateListWithProgress()
                }
            }
        }

        override fun getItemCount(): Int {
            return fileItemList.size
        }

        fun asyncUpdateListWithProgress() {
            val progressManager = outer.progressManager
            progressManager.show()
            asyncUpdateData({ type, current, total ->
                outer.runOnUiThread {
                    progressManager.update(type, current, total)
                }
            }, {
                outer.runOnUiThread {
                    notifyDataSetChanged()
                    progressManager.dismiss()
                    onPathChangedListener?.invoke(getCurrentPath())
                    outer.recyclerView.smoothScrollToPosition(0)
                }
            })
        }
    }

    class ProgressDialogManager(private val context: Context) {
        private val content = View.inflate(context, R.layout.magic_progress_dialog, null)
        private val bindings = MagicProgressDialogBinding.bind(content)
        private val msgTV = bindings.msgTv

        private val dialog = Dialog(context).apply {
            setContentView(content)
            setCanceledOnTouchOutside(true)
            setCancelable(false)
        }

        init {
            dialog.setContentView(content)
        }

        fun show() {
            msgTV.setText(R.string.nul)
            dialog.show()
        }

        fun update(type: MyAdapter.ProgressType, current: Int, total: Int) {
            val msgStr = when (type) {
                MyAdapter.ProgressType.LIST -> context.getString(R.string.magic_progress_listing)
                MyAdapter.ProgressType.DESCRIBE -> context.getString(
                    R.string.magic_progress_describing,
                    current,
                    total
                )

                MyAdapter.ProgressType.SORT -> context.getString(R.string.magic_progress_sorting)
                MyAdapter.ProgressType.DONE -> context.getString(R.string.magic_progress_done)
            }
            msgTV.text = msgStr
        }

        fun dismiss() {
            dialog.dismiss()
        }
    }
}

typealias OnPathChangedListener = (newPath: String) -> Unit
typealias ProgressCallback = (type: FileListActivity.MyAdapter.ProgressType, current: Int, total: Int) -> Unit
