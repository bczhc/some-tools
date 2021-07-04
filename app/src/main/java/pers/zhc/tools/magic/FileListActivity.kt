package pers.zhc.tools.magic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.magic_file_list_activity.*
import kotlinx.android.synthetic.main.magic_file_list_item.view.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        magicDatabase = File(filesDir, "magic.mgc")
        magic = Magic()
        // TODO: 7/4/21 check digest (when magic.mgc has a bad integrity, then the activity will keep crashing
        if (/*!magicDatabase.exists()*/ true/* every time download and overwrite (workaround) */) {

            DialogUtil.createConfirmationAlertDialog(this, { _, _ ->

                runOnUiThread {
                    downloadAndLoad()
                }

            }, { _, _ -> finish() }, R.string.magic_missing_database_dialog).show()

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

        pathTV.text = recyclerAdapter.getCurrentPath()
        recyclerAdapter.setOnPathChangedListener {
            pathTV.text = it
        }
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
        val previous = recyclerAdapter.previous()
        if (previous == null) {
            super.onBackPressed()
        }
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

    class MyAdapter(private val context: Context, private val magic: Magic) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private var onPathChangedListener: OnPathChangedListener? = null
        private var currentPathFile: File
        private val fileItemList = ArrayList<FileItem>()

        init {
            currentPathFile = File(Common.getExternalStoragePath(context))
            updateList()
        }

        private fun setPath(path: String) {
            currentPathFile = File(path)
            onPathChangedListener?.invoke(getCurrentPath())
        }

        private fun updateList() {
            fileItemList.clear()
            currentPathFile.listFiles()?.forEach {
                val info = magic.file(it.path)
                fileItemList.add(
                    FileItem(
                        it.name,
                        info,
                        if (it.isDirectory) {
                            FileType.FOLDER
                        } else {
                            FileType.FILE
                        },
                        it
                    )
                )
            }
            fileItemList.sortWith { fileItem, fileItem2 ->
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
            notifyDataSetChanged()
        }

        /**
         * Returns the path after doing a "previous" operation
         */
        fun previous(): String? {
            val parentFile = currentPathFile.parentFile ?: return null
            currentPathFile = parentFile
            updateList()
            onPathChangedListener?.invoke(getCurrentPath())
            return currentPathFile.path
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
            val inflate = LayoutInflater.from(context).inflate(R.layout.magic_file_list_item, parent, false)
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
                    updateList()
                }
            }
        }

        override fun getItemCount(): Int {
            return fileItemList.size
        }
    }
}

typealias OnPathChangedListener = (newPath: String) -> Unit