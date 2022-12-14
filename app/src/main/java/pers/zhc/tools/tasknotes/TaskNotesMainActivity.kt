package pers.zhc.tools.tasknotes

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
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
                    }
                    return@setOnMenuItemClickListener true
                }
            }.show()
        }

        showNotification(notificationId)
    }

    private fun queryAndSetListItems() {
        listItems.clear()
        listItems.addAll(database.queryAll())
    }

    private fun showDeleteRecordDialog(record: Record) {
        DialogUtils.createConfirmationAlertDialog(
            this, positiveAction = { _, _ ->
                database.delete(record.time)
                ToastUtils.show(this, R.string.deleting_succeeded)
                val index = listItems.indexOfFirst { it.time == record.time }
                androidAssert(index != -1)
                listItems.removeAt(index)
                listAdapter.notifyItemRemoved(index)
            }, titleRes = R.string.whether_to_delete,
            width = ViewGroup.LayoutParams.MATCH_PARENT, message = record.description
        ).show()
    }

    @Suppress("DuplicatedCode")
    private fun showNotification(notificationId: Int) {
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
        nm.notify(notificationId, notification)
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
            val (description, mark, time) = records[position]
            holder.descriptionTV.text = description
            holder.taskMarkTV.text = context.getString(mark.getStringRes())
            holder.timeTV.text = formatTime(time)
        }

        @SuppressLint("SimpleDateFormat")
        fun formatTime(timestamp: Long): String =
            SimpleDateFormat("MM-dd HH:mm").format(Date(timestamp))


        override fun getItemCount() = records.size
    }

    companion object {
        private val notificationId by lazy { System.currentTimeMillis().hashCode() }
    }
}

typealias Records = ArrayList<Record>
