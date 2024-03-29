package pers.zhc.tools.exifviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.MyApplication.Companion.GSON
import pers.zhc.tools.R
import pers.zhc.tools.databinding.ExifViewerDetailedInfoDialogBinding
import pers.zhc.tools.databinding.ExifViewerDetailedInfoDialogImport1Binding
import pers.zhc.tools.databinding.ExifViewerEntryItemBinding
import pers.zhc.tools.databinding.ExifViewerMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import java.io.File

class MainActivity : BaseActivity() {
    private lateinit var bindings: ExifViewerMainBinding
    private var exifEntries = ExifEntries()
    private lateinit var listAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ExifViewerMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        listAdapter = ListAdapter(exifEntries)
        bindings.recyclerView.apply {
            addDividerLines()
            setLinearLayoutManager()
            adapter = listAdapter
        }

        // launched from the "Open as" dialog
        checkFromOpenAs()?.let { result ->
            val stream = result.openInputStream()
            if (stream == null) {
                ToastUtils.show(this, R.string.file_open_failed_toast)
                finish()
                return
            }
            bindings.filePathTv.text = result.uri.path
            // TODO: can we consider directly pass the stream to JNI?
            val tmpFile = tmpFile()
            tmpFile.copyFrom(stream)
            stream.close()
            updateResult(tmpFile)
            tmpFile.delete()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateResult(file: File) {
        runCatching {
            val exifInfo = JNI.Exif.getExifInfo(file.path)
            exifEntries.clear()
            exifEntries.addAll(GSON.fromJson(exifInfo, Array<ExifEntry>::class.java))
            listAdapter.notifyDataSetChanged()
        }.toastOnFailure(this)
    }

    class ListAdapter(val entries: ExifEntries) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
        class ViewHolder(bindings: ExifViewerEntryItemBinding) : RecyclerView.ViewHolder(bindings.root) {
            val tv = bindings.textView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val bindings = ExifViewerEntryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(bindings)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val context = holder.tv.context
            val entry = entries[position]
            val text = "${entry.tagDesc}: ${entry.valueReadable}"
            holder.tv.text = text

            holder.tv.setOnClickListener {
                val titles = arrayOf(
                    "Tag ID",
                    "Tag Display",
                    "Tag Description",
                    "Value Readable",
                    "Value Display",
                    "Value Internal",
                )
                val values = arrayOf(
                    "0x${entry.tagId.toString(16).completeLeadingZeros(4)}",
                    entry.tagDisplay,
                    entry.tagDesc,
                    entry.valueReadable,
                    entry.valueDisplay,
                    entry.valueInternal,
                )

                val bindings = ExifViewerDetailedInfoDialogBinding.inflate(LayoutInflater.from(context))
                bindings.includesParent.children.forEachIndexed { index, v ->
                    androidAssert(v is LinearLayout)
                    ExifViewerDetailedInfoDialogImport1Binding.bind(v).apply {
                        clickableLl.setOnLongClickListener {
                            ClipboardUtils.putWithToast(MyApplication.appContext, values[index])
                            true
                        }
                        titleTv.text = titles[index]
                        contentTv.text = values[index]
                    }
                }

                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.exif_detailed_info_dialog_title)
                    .setView(bindings.root)
                    .show()
            }
        }

        override fun getItemCount(): Int {
            return entries.size
        }
    }
}

typealias ExifEntries = ArrayList<ExifEntry>
