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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.magic_file_list_activity.*
import kotlinx.android.synthetic.main.magic_file_list_item.view.*
import kotlinx.android.synthetic.main.magic_progress_dialog.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.Download
import java.io.File
import java.net.URL

/**
 * @author bczhc
 */
class FileListActivity : BaseActivity() {
    private lateinit var recyclerAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var magic: Magic
    private lateinit var magicDatabase: File
    private lateinit var progressShower: ProgressDialogShower

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        magicDatabase = File(filesDir, "magic.mgc")
        magic = Magic()
        progressShower = ProgressDialogShower(this)

        // TODO: 7/4/21 check digest (when magic.mgc has a bad integrity, then the activity will keep crashing
        if (/*!magicDatabase.exists()*/ true/* every time download and overwrite (workaround) */) {

            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->

                runOnUiThread {
                    downloadAndLoad()
                }

            }, { _, _ -> finish() }, R.string.magic_missing_database_dialog).apply {
                setCanceledOnTouchOutside(false)
                setCancelable(false)
            }.show()

        } else {
            load()
        }
    }

    private fun downloadAndLoad() {
        Download.startDownloadWithDialog(
            this, URL(Common.getStaticResourceUrlString("magic.mgc")),
            File(filesDir, "magic.mgc")
        ) {
            runOnUiThread {
                load()
            }
        }
    }

    fun load() {
        setContentView(R.layout.magic_file_list_activity)

        recyclerView = file_list_rv!!
        val pathTV = path_tv!!

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

    class FileItem(
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

            result.sortWith { fileItem, fileItem2 ->
                if (fileItem.fileType == fileItem2.fileType) {
                    if (!fileItem.file.isHidden && !fileItem2.file.isHidden) {
                        return@sortWith fileItem.fileName.compareTo(fileItem2.fileName)
                    } else {
                        return@sortWith if (fileItem.file.isHidden) {
                            1
                        } else {
                            -1
                        }
                    }
                } else {
                    return@sortWith if (fileItem.fileType == FileType.FILE) {
                        1
                    } else {
                        -1
                    }
                }
            }

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
            val filenameTV: TextView = view.filename_tv!!
            val fileInfoTV: TextView = view.file_info_tv!!
            val fileTypeIV: ImageView = view.iv!!
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
            val progressShower = outer.progressShower
            progressShower.show()
            asyncUpdateData({ type, current, total ->
                progressShower.update(type, current, total)
            }, {
                outer.runOnUiThread {
                    notifyDataSetChanged()
                    progressShower.dismiss()
                    onPathChangedListener?.invoke(getCurrentPath())
                    outer.recyclerView.smoothScrollToPosition(0)
                }
            })
        }
    }

    class ProgressDialogShower(private val context: Context) {
        private val content = View.inflate(context, R.layout.magic_progress_dialog, null)
        private val msgTV = content.msg_tv!!

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