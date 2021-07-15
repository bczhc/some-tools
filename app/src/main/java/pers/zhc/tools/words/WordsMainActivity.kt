package pers.zhc.tools.words

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
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

        if (database == null) {
            // init
            database = SQLite3.open(Common.getInternalDatabaseDir(this, "words.db").path)
            initDatabase(database!!)
        }

        val intent = Intent(this, DialogShowActivity::class.java)
        val pi = PendingIntent.getActivity(this, RequestCode.START_ACTIVITY_0, intent, 0)

        val notification = NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setTitle(R.string.words_label)
            setContentIntent(pi)
        }.build()

        val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(0, notification)
    }

    private fun initDatabase(database: SQLite3) {
        database.exec(
            """CREATE TABLE IF NOT EXISTS words
(
    word          TEXT NOT NULL,
    addition_time INTEGER
)"""
        )
    }

    companion object {
        var database: SQLite3? = null
    }
}