package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.sqlite.MySQLite3
import java.io.File

class DiaryAttachmentActivity : BaseActivity() {
    companion object {
        var db: MySQLite3? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_attachment_activity)
        setDatabase()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_attachment_actionbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, DiaryAttachmentAddingActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setDatabase() {
        val dbFile = File(Common.getExternalStoragePath(this), "diary-attachment.db")
        db = MySQLite3.open(dbFile.path)
        db!!.exec("CREATE TABLE IF NOT EXISTS diary_attachment\n(\n    title                TEXT NOT NULL,\n    file_path_json_array TEXT NOT NULL,\n    description          TEXT NOT NULL,\n    type                 INTEGER\n);");
    }
}