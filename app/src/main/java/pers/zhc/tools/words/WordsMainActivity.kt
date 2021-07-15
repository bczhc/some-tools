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
import pers.zhc.tools.utils.Common
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
    }

    class Item(
        val word: String
    )

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

    class MyAdapter(private val ctx: Context, private val itemList: List<Item>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var tv: TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
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