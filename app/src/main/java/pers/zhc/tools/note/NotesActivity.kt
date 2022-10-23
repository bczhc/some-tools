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
import androidx.activity.result.contract.ActivityResultContract
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_item.view.*
import kotlinx.android.synthetic.main.notes_activity.*
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
            val position = itemData.indexOfFirst {
                it.time == timestamp
            }
            androidAssert(position != -1)
            itemData[position] = database.query(timestamp)!!
            listAdapter.notifyItemChanged(position)
        }
    }

    private lateinit var listAdapter: ListAdapter
    private var itemData = ArrayList<Record>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notes_activity)

        val recyclerView = recycler_view!!
        listAdapter = ListAdapter()
        recyclerView.adapter = listAdapter
        recyclerView.setLinearLayoutManager()

        listAdapter.setOnItemLongClickListener { position, view ->
            val record = itemData[position]

            val menu = PopupMenuUtil.create(this, view, R.menu.note_item_menu)
            menu.show()
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.modify -> {
                        launchers.modify.launch(record.time)
                    }
                    R.id.delete -> {
                        delete(record.time, position)
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }

        Thread {
            updateAllRecords()
            listAdapter.notifyDataSetChanged()
        }.start()
    }

    private fun delete(timestamp: Long, position: Int) {
        DialogUtils.createConfirmationAlertDialog(
            this,
            titleRes = R.string.delete_confirmation_dialog_title,
            positiveAction = { _, _ ->
                database.deleteRecord(timestamp)
                itemData.removeAt(position)
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
                launchers.import.launch(FilePicker.PICK_FILE)
            }
            R.id.export -> {
                launchers.export.launch(FilePicker.PICK_FOLDER)
            }
        }
        return true
    }

    private fun updateAllRecords() {
        itemData = database.queryAll()
    }

    private fun import(path: File) {
        // TODO: database reference checking
        FileUtil.copy(path, Database.databasePath)
    }

    private fun export(dest: File) {
        FileUtil.copy(Database.databasePath, dest)
        ToastUtils.show(this, R.string.exporting_succeeded)
    }

    private inner class ListAdapter : AdapterWithClickListener<ListAdapter.ViewHolder>() {
        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateTV = view.date_tv!!
            val titleTV = view.title_tv!!
            val contentTV = view.content_tv!!
        }

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val inflate = LayoutInflater.from(this@NotesActivity).inflate(R.layout.note_item, parent, false)
            return ViewHolder(inflate)
        }

        override fun getItemCount(): Int {
            return itemData.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = itemData[position]
            holder.dateTV.text = Date(record.time).toString()
            holder.titleTV.text = StringUtils.limitText(record.title)
            holder.contentTV.text = StringUtils.limitText(record.content)
        }
    }
}