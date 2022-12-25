package pers.zhc.tools.tasknotes

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TaskNotesListItemBinding
import pers.zhc.tools.databinding.TaskNotesMainBinding
import pers.zhc.tools.utils.*
import java.text.SimpleDateFormat
import java.util.*

class TaskNotesMainActivity : BaseActivity() {
    private val listItems = Records()
    private val database by lazy { Database.database }
    private lateinit var listAdapter: ListAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var onRecordAddedReceiver: OnRecordAddedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TaskNotesMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val recyclerView = bindings.recyclerView
        queryAndSetListItems()
        listAdapter = ListAdapter()
        itemTouchHelper = ItemTouchHelper(ListTouchHelperCallback(listAdapter, database))
        recyclerView.apply {
            adapter = listAdapter
            setLinearLayoutManager()
            setUpFastScroll(this@TaskNotesMainActivity)
            itemTouchHelper.attachToRecyclerView(this)
        }

        listAdapter.setOnItemClickListener { position, view ->
            PopupMenuUtil.create(this, view, R.menu.task_notes_list_item_popup).apply {
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.delete -> {
                            showDeleteRecordDialog(listItems[position])
                        }

                        R.id.recreate -> {
                            recreateTaskRecord(listItems[position])
                        }

                        R.id.modify -> {
                            showModifyRecordDialog(listItems[position])
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }.show()
        }
        listAdapter.setOnItemLongClickListener(listAdapter.getOnItemClickListener())

        showNotification()

        onRecordAddedReceiver = OnRecordAddedReceiver { creationTime ->
            val record = database.query(creationTime)!!
            listItems.add(record)
            listAdapter.notifyItemInserted(listItems.size - 1)
        }.also {
            registerReceiver(it, IntentFilter().apply {
                addAction(OnRecordAddedReceiver.ACTION_RECORD_ADDED)
            })
        }
    }

    private fun onRecordAdded(record: Record) {
        database.insert(record)
        listItems.add(record)
        listAdapter.notifyItemInserted(listItems.size - 1)
        ToastUtils.show(this, R.string.adding_succeeded)
    }

    private fun showModifyRecordDialog(record: Record) {
        Dialog.createRecordEditDialog(this, record) { newRecord ->
            newRecord ?: return@createRecordEditDialog
            database.update(record.creationTime, newRecord)
            val index = listItems.indexOfFirst { it.creationTime == record.creationTime }
            androidAssert(index != -1)
            listItems[index] = newRecord
            listAdapter.notifyItemChanged(index)
        }
    }

    private fun recreateTaskRecord(record: Record) {
        Dialog.createRecordEditDialog(this, record, recreateMode = true) { createdRecord ->
            createdRecord ?: return@createRecordEditDialog
            onRecordAdded(createdRecord)
        }
    }

    private fun queryAndSetListItems(today: Boolean = true) {
        listItems.clear()
        if (today) {
            listItems.addAll(database.queryToday())
        } else {
            // the `groupBy` prevents the original sequence order,
            // so there's no need to sort by the order ordinal number again
            database.withQueryAll { rows ->
                val records = rows.asSequence()
                    .map { Pair(IntDate(Date(it.creationTime)), it) }
                    .groupBy { it.first.dateInt }
                    .entries.sortedBy { it.key }
                    .flatMap { it.value.map { x -> x.second } }
                listItems.addAll(records)
            }
        }
    }

    private fun showDeleteRecordDialog(record: Record) {
        DialogUtils.createConfirmationAlertDialog(
            this, positiveAction = { _, _ ->
                database.delete(record.creationTime)
                ToastUtils.show(this, R.string.deleting_succeeded)
                val index = listItems.indexOfFirst { it.creationTime == record.creationTime }
                androidAssert(index != -1)
                listItems.removeAt(index)
                listAdapter.notifyItemRemoved(index)
            }, titleRes = R.string.whether_to_delete,
            width = ViewGroup.LayoutParams.MATCH_PARENT, message = record.description
        ).show()
    }

    @Suppress("DuplicatedCode")
    private fun showNotification() {
        val intent = Intent(this, DialogShowActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val notification = NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(getString(R.string.task_notes_add_task_record_notification_title))
            setContentIntent(pi)
            setOngoing(true)
        }.build()

        val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun finish() {
        unregisterReceiver(onRecordAddedReceiver)
        super.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.task_notes_main, menu)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_today -> {
                item.isChecked = true
                queryAndSetListItems()
                listAdapter.notifyDataSetChanged()
            }

            R.id.show_all -> {
                item.isChecked = true
                queryAndSetListItems(today = false)
                listAdapter.notifyDataSetChanged()
            }

            else -> {}
        }
        return true
    }

    inner class ListAdapter :
        AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        private val context = this@TaskNotesMainActivity as Context
        val records = this@TaskNotesMainActivity.listItems

        @SuppressLint("ClickableViewAccessibility")
        inner class MyViewHolder(view: View) : ViewHolder(view) {
            private val bindings = TaskNotesListItemBinding.bind(view)
            val descriptionTV = bindings.descriptionTv
            val taskMarkTV = bindings.taskMarkTv
            val timeTV = bindings.timeTv
            private val dragIcon = bindings.dragIcon

            init {
                dragIcon.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(this)
                    }
                    false
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val inflate = TaskNotesListItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return MyViewHolder(inflate.root)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val (description, mark, time, creationTime) = records[position]
            holder.descriptionTV.text = description
            holder.taskMarkTV.text = context.getString(mark.getStringRes())
            holder.timeTV.text = formatTime(creationTime, time)
        }

        @SuppressLint("SimpleDateFormat")
        fun formatTime(creationTime: Long, time: Time): String {
            val calendar = Calendar.getInstance().apply {
                this.time = Date(creationTime)
                val year = get(Calendar.YEAR)
                val month = get(Calendar.MONTH)
                val day = get(Calendar.DAY_OF_MONTH)
                set(year, month, day, time.hour, time.minute)
            }
            return SimpleDateFormat("MM-dd HH:mm").format(calendar.time)
        }


        override fun getItemCount() = records.size
    }

    private class ListTouchHelperCallback(private val listAdapter: ListAdapter, private val database: Database) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
            val dragFlag = ItemTouchHelper.UP xor ItemTouchHelper.DOWN
            return makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, 0)
                .xor(makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, dragFlag))
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
            val fromIndex = viewHolder.layoutPosition
            val toIndex = target.layoutPosition
            Collections.swap(listAdapter.records, fromIndex, toIndex)
            listAdapter.notifyItemMoved(fromIndex, toIndex)
            return true
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
            database.reorderRecords(listAdapter.records)
        }

        override fun isLongPressDragEnabled() = false
    }

    companion object {
        /**
         * randomly assigned
         */
        private const val NOTIFICATION_ID = 1955794286
    }
}

typealias Records = ArrayList<Record>
