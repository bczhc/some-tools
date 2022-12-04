package pers.zhc.tools.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.R
import pers.zhc.tools.databinding.NoteItemBinding
import pers.zhc.tools.databinding.NotesActivityBinding
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
            override fun createIntent(context: Context, input: Unit): Intent {
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
            override fun createIntent(context: Context, input: Long): Intent {
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
    private lateinit var chooseAllOnCheckedAction: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
    private lateinit var bindings: NotesActivityBinding

    /**
     * multi-selection action mode
     */
    private var actionMode: ActionMode? = null
    private val inActionMode
        get() = actionMode != null

    // TODO: extract and encapsulate the top batch deletion bar
    private var selectedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = NotesActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbar)

        listAdapter = ListAdapter()

        bindings.recyclerView.apply {
            adapter = listAdapter
            setLinearLayoutManager()
            FastScrollerBuilder(this).apply {
                setThumbDrawable(AppCompatResources.getDrawable(this@NotesActivity, R.drawable.thumb)!!)
            }.build()
        }


        listAdapter.setOnItemLongClickListener { position, v ->
            if (!inActionMode) {
                androidAssert(actionMode == null)
                actionMode = startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
                        menuInflater.inflate(R.menu.note_multi_selection_action_mode, menu)
                        (menu.findItem(R.id.select_all).actionView as MaterialCheckBox).setOnCheckedChangeListener(
                            chooseAllOnCheckedAction
                        )
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        return true
                    }

                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.delete -> {
                                batchDeleteSelected()
                            }
                        }
                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode?) {
                        actionMode = null
                        clearSelectionState()
                    }
                })!!

                // first select the current one
                listItems[position].selected = true
                listAdapter.notifyItemChanged(position)
                ++selectedCount
                androidAssert(selectedCount == 1)

                actionMode!!.title = getString(R.string.note_selected_notes_count_msg, selectedCount)
            } else {
                // long press in multi-selection mode
                // equivalent to single-clicking the item (select it)
                listAdapter.getOnItemClickListener()!!(position, v)
            }
        }

        listAdapter.setOnItemClickListener { position, _ ->
            val item = listItems[position]
            if (inActionMode) {
                item.selected = !item.selected
                listAdapter.notifyItemChanged(position)
                if (item.selected) {
                    ++selectedCount
                } else {
                    --selectedCount
                }

                if (selectedCount == 0) {
                    actionMode!!.finish()
                    return@setOnItemClickListener
                }

                updateActionModeTitle()

                (actionMode!!.menu.findItem(R.id.select_all).actionView as MaterialCheckBox).apply {
                    setOnCheckedChangeListener(null)
                    isChecked = selectedCount == listItems.size
                    setOnCheckedChangeListener(chooseAllOnCheckedAction)
                }
            } else {
                launchers.modify.launch(item.data.time)
            }
        }

        Thread {
            updateAllRecords()
            listAdapter.notifyDataSetChanged()
        }.start()

        chooseAllOnCheckedAction = { _, isChecked ->
            for (listItem in listItems) {
                listItem.selected = isChecked
            }
            selectedCount = if (isChecked) {
                listItems.size
            } else {
                0
            }
            listAdapter.notifyItemRangeChanged(0, listItems.size)
            updateActionModeTitle()
        }
    }

    private fun clearSelectionState() {
        selectedCount = 0
        for (item in listItems) {
            item.selected = false
        }
        listAdapter.notifyItemRangeChanged(0, listItems.size)
    }

    private fun updateActionModeTitle() {
        actionMode!!.title = getString(R.string.note_selected_notes_count_msg, selectedCount)
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

                finishActionMode()
            }).show()
    }

    private fun finishActionMode() {
        actionMode!!.finish()
        actionMode = null
        clearSelectionState()
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

                var msgResOnFinished = 0
                SQLite3::class.withNew(Database.databasePath.path) {
                    if (it.checkIfCorrupt()) {
                        msgResOnFinished = R.string.corrupted_database_and_recreate_new_msg
                        Database.databasePath.requireDelete()
                    }
                }

                reopenDatabase()

                updateAllRecords()
                listAdapter.notifyDataSetChanged()

                if (msgResOnFinished == 0) {
                    msgResOnFinished = R.string.importing_succeeded
                }
                ToastUtils.show(this, msgResOnFinished)

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
            val bindings = NoteItemBinding.bind(view)

            val dateTV = bindings.dateTv
            val titleTV = bindings.titleTv
            val contentTV = bindings.contentTv
            val itemLL = bindings.itemLl
        }

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val inflate = LayoutInflater.from(this@NotesActivity).inflate(R.layout.note_item, parent, false)
            return ViewHolder(inflate)
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        private val dateTimeFormatter = SimpleDateFormat.getDateTimeInstance()

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = listItems[position]
            val record = item.data
            holder.dateTV.text = dateTimeFormatter.format(Date(record.time)).toString()
            holder.titleTV.text = StringUtils.limitText(record.title)
            holder.contentTV.text = StringUtils.limitText(record.content)
            holder.itemLL.setBackgroundResource(
                if (item.selected) {
                    R.drawable.clickable_view_selected

                } else {
                    R.drawable.selectable_bg
                }
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create -> {
                launchers.create.launch(Unit)
            }
            R.id.import_ -> {
                actionMode?.finish()
                launchers.import.launch(FilePicker.PICK_FILE)
            }
            R.id.export -> {
                actionMode?.finish()
                launchers.export.launch(FilePicker.PICK_FOLDER)
            }
        }
        return true
    }
}
