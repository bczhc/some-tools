package pers.zhc.tools.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_item.view.*
import kotlinx.android.synthetic.main.note_top_view.view.*
import kotlinx.android.synthetic.main.notes_activity.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.*
import java.io.File
import java.util.Date

class NotesActivity : NoteBaseActivity() {
    private val launchers = object {
        val import = FilePicker.getLauncher(this@NotesActivity) {
            it ?: return@getLauncher

            import(File(it))
        }

        val export = FilePicker.getLauncherWithFilename(this@NotesActivity) { path, filename ->
            path ?: return@getLauncherWithFilename
            if (filename.isEmpty()) return@getLauncherWithFilename

            val dest = File(path, filename)
            export(dest)
        }

        val create = registerForActivityResult(object : ActivityResultContract<Unit, Unit>() {
            override fun createIntent(context: Context, input: Unit?): Intent {
                return Intent(context, NoteTakingActivity::class.java).apply {
                    putExtra(NoteTakingActivity.EXTRA_TYPE, NoteTakingActivity.Type.CREATE)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?) {
            }
        }) {
            updateAllRecords()
            listAdapter.notifyDataSetChanged()
        }

        val modify = registerForActivityResult(object : ActivityResultContract<Long, Long>() {
            override fun createIntent(context: Context, input: Long?): Intent {
                return Intent(context, NoteTakingActivity::class.java).apply {
                    putExtra(NoteTakingActivity.EXTRA_TYPE, NoteTakingActivity.Type.UPDATE)
                    putExtra(NoteTakingActivity.EXTRA_TIMESTAMP, input)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Long {
                return intent!!.getLongExtra(NoteTakingActivity.EXTRA_TIMESTAMP, 0)
            }
        }) { timestamp ->
            val position = listItems.indexOfFirst {
                it.data.time == timestamp
            }
            androidAssert(position != -1)
            listItems[position] = ListItem(database.query(timestamp)!!)
            listAdapter.notifyItemChanged(position)
        }
    }

    private lateinit var listAdapter: ListAdapter
    private var listItems = ArrayList<ListItem>()
    private lateinit var topDeleteBarLL: FrameLayout
    private var onDeleting = false

    // TODO: extract and encapsulate the top batch deletion bar
    private var deleteSelectedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notes_activity)

        topDeleteBarLL = top_ll!!
        val recyclerView = recycler_view!!
        listAdapter = ListAdapter()
        recyclerView.adapter = listAdapter
        recyclerView.setLinearLayoutManager()

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(this@NotesActivity, R.drawable.thumb)!!)
        }.build()

        listAdapter.setOnItemLongClickListener { position, view ->
            if (onDeleting) {
                // in deletion mode, delete all selected
                batchDeleteSelected()
                return@setOnItemLongClickListener
            }
            val record = listItems[position]

            val menu = PopupMenuUtil.create(this, view, R.menu.note_item_menu)
            menu.show()
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        delete(record.data.time, position)
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }

        listAdapter.setOnItemClickListener { position, _ ->
            val item = listItems[position]
            if (onDeleting) {
                item.selected = !item.selected
                listAdapter.notifyItemChanged(position)
                if (item.selected) {
                    ++deleteSelectedCount
                } else {
                    --deleteSelectedCount
                }
                updateDeleteTopTV()
            } else {
                launchers.modify.launch(item.data.time)
            }
        }

        Thread {
            updateAllRecords()
            listAdapter.notifyDataSetChanged()
        }.start()
    }

    private fun updateDeleteTopTV() {
        androidAssert(topDeleteBarLL.childCount == 1)
        val topCountTV = topDeleteBarLL.getChildAt(0).top_tv!!
        topCountTV.text = if (deleteSelectedCount == 0) {
            getString(R.string.note_no_notes_selected_msg)
        } else {
            getString(R.string.note_selected_notes_count_msg, deleteSelectedCount)
        }
    }

    private fun batchDeleteSelected() {
        DialogUtils.createConfirmationAlertDialog(
            this,
            titleRes = R.string.delete_confirmation_dialog_title,
            width = MATCH_PARENT,
            positiveAction = { _, _ ->
                database.batchDelete(listItems.filter { it.selected }.map { it.data.time }.iterator())

                val sizeBefore = listItems.size
                listItems.removeAll { it.selected }
                val deleteCount = sizeBefore - listItems.size
                listAdapter.notifyDataSetChanged()
                ToastUtils.show(this, getString(R.string.note_batch_deleted_notes_done_toast, deleteCount))
                exitDeletionMode()
            }).show()
    }

    private fun exitDeletionMode() {
        if (!onDeleting) return
        topDeleteBarLL.removeAllViews()
        for (item in listItems) {
            item.selected = false
        }
        listAdapter.notifyDataSetChanged()
        onDeleting = false
    }

    private fun delete(timestamp: Long, position: Int) {
        DialogUtils.createConfirmationAlertDialog(
            this,
            titleRes = R.string.delete_confirmation_dialog_title,
            positiveAction = { _, _ ->
                database.deleteRecord(timestamp)
                listItems.removeAt(position)
                listAdapter.notifyItemRemoved(position)
            }, width = MATCH_PARENT
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.note_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create -> {
                launchers.create.launch(Unit)
            }
            R.id.import_ -> {
                exitDeletionMode()
                launchers.import.launch(FilePicker.PICK_FILE)
            }
            R.id.export -> {
                exitDeletionMode()
                launchers.export.launch(FilePicker.PICK_FOLDER)
            }
            R.id.delete -> {
                batchDeleteAction()
            }
        }
        return true
    }

    private fun batchDeleteAction() {
        deleteSelectedCount = 0
        if (onDeleting) {
            exitDeletionMode()
        } else {
            onDeleting = true
            val topView = View.inflate(this, R.layout.note_top_view, null)
            topDeleteBarLL.removeAllViews()
            topDeleteBarLL.addView(topView)

            val chooseAllBtn = topView.choose_all!!
            val cancelBtn = topView.cancel_deletion!!
            chooseAllBtn.setOnCheckedChangeListener { _, isChecked ->
                for (listItem in listItems) {
                    listItem.selected = isChecked
                }
                deleteSelectedCount = if (isChecked) {
                    listItems.size
                } else {
                    0
                }
                listAdapter.notifyDataSetChanged()
                updateDeleteTopTV()
            }
            cancelBtn.setOnClickListener {
                exitDeletionMode()
            }
        }
    }

    private fun updateAllRecords() {
        listItems = ArrayList(database.queryAll().map { ListItem(it) })
    }

    private fun import(path: File) {
        DialogUtils.createConfirmationAlertDialog(
            this,
            titleRes = R.string.import_dialog,
            message = getString(R.string.note_import_overwrite_alert),
            width = MATCH_PARENT,
            positiveAction = { _, _ ->

                val refCount = Database.getRefCount()
                // there are still other references (> 1); cannot import
                if (refCount != 1) {
                    ToastUtils.show(
                        this,
                        getString(R.string.note_import_ref_count_not_zero_msg, refCount)
                    )
                    return@createConfirmationAlertDialog
                }
                // release the current database
                databaseRef.release()
                androidAssert(Database.getRefCount() == 0)

                FileUtil.copy(path, Database.databasePath)

                reopenDatabase()

                updateAllRecords()
                listAdapter.notifyDataSetChanged()

                ToastUtils.show(this, R.string.importing_succeeded)

            }).show()
    }

    private fun export(dest: File) {
        val export = {
            FileUtil.copy(Database.databasePath, dest)
            ToastUtils.show(this, R.string.exporting_succeeded)
        }

        if (dest.exists()) {
            DialogUtils.createConfirmationAlertDialog(
                this,
                titleRes = R.string.export_dialog,
                message = getString(R.string.note_filename_duplication_alert),
                width = MATCH_PARENT,
                positiveAction = { _, _ ->
                    export()
                }
            ).show()
        } else export()
    }

    private data class ListItem(
        val data: Record,
        var selected: Boolean,
    ) {
        constructor(record: Record) : this(record, false)
    }

    private inner class ListAdapter : AdapterWithClickListener<ListAdapter.ViewHolder>() {
        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateTV = view.date_tv!!
            val titleTV = view.title_tv!!
            val contentTV = view.content_tv!!
            val borderLL = view.border_ll!!
        }

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val inflate = LayoutInflater.from(this@NotesActivity).inflate(R.layout.note_item, parent, false)
            return ViewHolder(inflate)
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = listItems[position]
            val record = item.data
            holder.dateTV.text = Date(record.time).toString()
            holder.titleTV.text = StringUtils.limitText(record.title)
            holder.contentTV.text = StringUtils.limitText(record.content)
            holder.borderLL.setBackgroundResource(
                if (item.selected) {
                    R.drawable.clickable_view_stroke_red
                } else {
                    R.drawable.selectable_bg
                }
            )
        }
    }
}