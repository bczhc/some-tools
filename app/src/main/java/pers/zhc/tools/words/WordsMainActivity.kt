package pers.zhc.tools.words

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.words_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.utils.AdapterWithClickListener
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.PopupMenuUtil
import pers.zhc.tools.utils.sqlite.SQLite3

/**
 * @author bczhc
 */
class WordsMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.words_activity)

        checkAndInitDatabase()
        showNotification()

        val recyclerView = recycler_view!!

        val itemList = queryItems()
        val listAdapter = MyAdapter(this, itemList)

        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        listAdapter.setOnItemLongClickListener { position, view ->

            val popupMenu = PopupMenuUtil.createPopupMenu(this, view, R.menu.word_item_popup_menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        DialogUtil.createConfirmationAlertDialog(this, { _, _ ->

                            deleteRecord(itemList[position].word)
                            itemList.removeAt(position)
                            listAdapter.notifyItemRemoved(position)

                        }, R.string.words_delete_confirmation_dialog).show()
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }
    }

    class Item(
        val word: String
    )

    private fun deleteRecord(word: String) {
        database!!.execBind("DELETE FROM word WHERE word IS ?", arrayOf(word))
    }

    private fun queryItems(): ArrayList<Item> {
        val itemList = ArrayList<Item>()

        val statement = database!!.compileStatement("SELECT word FROM word")
        val cursor = statement.cursor
        while (cursor.step()) {
            val word = cursor.getText(0)
            itemList.add(Item(word))
        }
        statement.release()

        return itemList
    }

    private fun showNotification() {
        val intent = Intent(this, DialogShowActivity::class.java)
        val pi = PendingIntent.getActivity(this, RequestCode.START_ACTIVITY_0, intent, 0)

        val notification = NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setTitle(R.string.words_label)
            setContentIntent(pi)
            setOngoing(true)
        }.build()

        val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(0, notification)
    }

    class MyAdapter(private val ctx: Context, private val itemList: List<Item>) :
        AdapterWithClickListener<MyAdapter.MyViewHolder>() {

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var tv: TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val inflate = View.inflate(ctx, android.R.layout.simple_list_item_1, null)
            val viewHolder = MyViewHolder(inflate)
            viewHolder.tv = inflate.findViewById(android.R.id.text1)!!
            return viewHolder
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.tv.text = itemList[position].word
        }

        override fun getItemCount(): Int {
            return itemList.size
        }
    }

    companion object {
        var database: SQLite3? = null
        private lateinit var databasePath: String

        fun init(ctx: Context) {
            databasePath = Common.getInternalDatabaseDir(ctx, "words.db").path
        }

        fun checkAndInitDatabase() {
            if (database == null) {
                // init
                database = SQLite3.open(databasePath)
                initDatabase(database!!)
            }
        }

        private fun initDatabase(database: SQLite3) {
            database.exec(
                """CREATE TABLE IF NOT EXISTS word
(
    word          TEXT NOT NULL PRIMARY KEY,
    addition_time INTEGER
)"""
            )
        }
    }
}