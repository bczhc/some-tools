package pers.zhc.tools.tasknotes

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TaskNotesListItemBinding
import pers.zhc.tools.databinding.TaskNotesMainBinding
import pers.zhc.tools.databinding.TaskNotesModifyRecordDialogBinding
import pers.zhc.tools.utils.*
import java.text.SimpleDateFormat
import java.util.*

class TaskNotesMainActivity : BaseActivity() {
    private val listItems = Records()
    private val database by lazy { Database.database }
    private lateinit var listAdapter: ListAdapter
    private lateinit var onRecordAddedReceiver: OnRecordAddedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TaskNotesMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val recyclerView = bindings.recyclerView
        recyclerView.setLinearLayoutManager()
        recyclerView.setUpFastScroll(this)
        queryAndSetListItems()
        listAdapter = ListAdapter(this, listItems)
        recyclerView.adapter = listAdapter

        listAdapter.setOnItemLongClickListener { position, view ->
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
        listAdapter.setOnItemClickListener(listAdapter.getOnItemLongClickListener())

        showNotification()

        onRecordAddedReceiver = OnRecordAddedReceiver { creationTime ->
            onRecordAdded(creationTime)
        }.also {
            registerReceiver(it, IntentFilter().apply {
                addAction(OnRecordAddedReceiver.ACTION_RECORD_ADDED)
            })
        }
    }

    private fun onRecordAdded(creationTime: Long) {
        queryAndSetListItems()
        val index = listItems.indexOfFirst { it.creationTime == creationTime }
        androidAssert(index != -1)
        listAdapter.notifyItemInserted(index)
    }

    private fun showModifyRecordDialog(record: Record) {
        val bindings = TaskNotesModifyRecordDialogBinding.inflate(layoutInflater)

        bindings.included.btg.check(
            when (record.mark) {
                TaskMark.START -> R.id.start
                TaskMark.END -> R.id.end
            }
        )
        bindings.included.descriptionEt.setText(record.description)
        bindings.timeTv.text = record.time.format()

        var newTime = record.time

        val updateRecord = {
            val description = bindings.included.descriptionEt.text.toString()
            val taskMark = when (bindings.included.btg.checkedButtonId) {
                R.id.start -> TaskMark.START
                R.id.end -> TaskMark.END
                else -> unreachable()
            }

            val newRecord = Record(
                description,
                taskMark,
                newTime,
                record.creationTime
            )
            database.update(record.creationTime, newRecord)

            val index = listItems.indexOfFirst { it.creationTime == record.creationTime }
            androidAssert(index != -1)
            listItems[index] = newRecord
            listAdapter.notifyItemChanged(index)
            ToastUtils.show(this, R.string.modification_succeeded)
        }

        MaterialAlertDialogBuilder(this)
            .setView(bindings.root)
            .setPositiveAction { _, _ ->
                updateRecord()
            }
            .setNegativeAction()
            .show()

        bindings.timeButton.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(CLOCK_24H)
                .setHour(record.time.hour)
                .setMinute(record.time.minute)
                .build().apply {
                    addOnPositiveButtonClickListener {
                        newTime = Time(hour, minute)
                        bindings.timeTv.text = newTime.format()
                    }
                }
                .show(supportFragmentManager, "Time Picker")
        }
    }

    private fun recreateTaskRecord(record: Record) {
        Dialog.createRecordAddingDialog(this, record.description) { createdRecord ->
            createdRecord ?: return@createRecordAddingDialog
            database.insert(createdRecord)
            onRecordAdded(createdRecord.creationTime)
        }
    }

    private fun queryAndSetListItems() {
        listItems.clear()
        listItems.addAll(database.queryAll())
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
            setContentTitle(getString(R.string.task_notes_label))
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

    private class ListAdapter(private val context: Context, private val records: Records) :
        AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        class MyViewHolder(view: View) : ViewHolder(view) {
            private val bindings = TaskNotesListItemBinding.bind(view)
            val descriptionTV = bindings.descriptionTv
            val taskMarkTV = bindings.taskMarkTv
            val timeTV = bindings.timeTv
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

    companion object {
        private const val NOTIFICATION_ID = 1955794286
    }
}

typealias Records = ArrayList<Record>
