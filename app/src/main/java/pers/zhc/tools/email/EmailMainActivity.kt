package pers.zhc.tools.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.email_main_activity.*
import org.json.JSONException
import org.json.JSONObject
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.readToString
import java.io.File

/**
 * @author bczhc
 */
class EmailMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.email_main_activity)
        database = Database()
        setupCurrentAccount()

        val subjectET = subject_et!!.editText
        val bodyET = body_et!!.editText
        val toET = to_et!!.editText
        val ccET = cc_et!!.editText
        val sendButton = send_button!!

        sendButton.setOnClickListener {
            // TODO: multi-to and multi-cc
            val message = Message(arrayOf(toET.text.toString()), subjectET.text.toString()).apply {
                ccET.text.toString().let {
                    if (it.isNotEmpty()) {
                        this.cc = arrayOf(it)
                    }
                }
                bodyET.text.toString().let {
                    if (it.isNotEmpty()) {
                        this.body = bodyET.text.toString()
                    }
                }
            }
            Thread {
                ToastUtils.show(this, R.string.email_sending_toast)
                try {
                    Sender.send(currentAccount!!, message)
                    ToastUtils.show(this, R.string.email_send_done_toast)
                } catch (e: Exception) {
                    ToastUtils.showError(this, R.string.email_send_failed, e)
                }
            }.start()
        }
    }

    override fun finish() {
        database.database.close()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.email_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.configure_account -> {
                startActivity(Intent(this, AccountSettingsActivity::class.java))
            }
            else -> {
            }
        }
        return true
    }

    private fun generateAccountJson(account: Account): JSONObject {
        val json = JSONObject()
        json.put("smtpServer", account.smtpTransport.server)
        json.put("username", account.smtpTransport.credential.username)
        json.put("password", account.smtpTransport.credential.password)
        json.put("headFrom", account.headerFrom)
        return json
    }

    private fun deserializeAccountJson(json: JSONObject): Account {
        val smtpServer = json.getString("smtpServer")
        val username = json.getString("username")
        val password = json.getString("password")
        val headFrom = json.getString("headFrom")
        return Account(SmtpTransport(smtpServer, Credential(username, password)), headFrom)
    }

    private fun setupCurrentAccount() {
        val accounts = database.queryAll()
        if (accounts.size == 0) {
            ToastUtils.show(this, R.string.email_no_account_toast)
            startActivity(Intent(this, AccountSettingsActivity::class.java))
            return
        }

        val jsonFile = File(filesDir, "email-account.json")
        if (!jsonFile.exists()) {
            jsonFile.createNewFile()
        }
        var read = jsonFile.readToString()
        try {
            // check
            JSONObject(read)
        } catch (e: JSONException) {
            read = "{}"
            jsonFile.writeText(read)
        }

        val json = JSONObject(read)
        if (!json.has("account")) {
            json.put("account", generateAccountJson(accounts[0]))
        }

        val accountJson = json.getJSONObject("account")
        val account = deserializeAccountJson(accountJson)
        currentAccount = account
    }

    class Database {
        var database: SQLite3 = SQLite3.open(databaseFile.path)

        init {
            configure()
        }

        private fun configure() {
            database.exec(
                """CREATE TABLE IF NOT EXISTS account
(
    id          INTEGER,
    smtp_server TEXT NOT NULL,
    username    TEXT NOT NULL,
    password    TEXT NOT NULL,
    header_from TEXT NOT NULL
)"""
            )
        }

        fun insert(account: Account) {
            val smtpTransport = account.smtpTransport
            val credential = smtpTransport.credential
            insert(smtpTransport.server, credential.username, credential.password, account.headerFrom)
        }

        private fun insert(smtpServer: String, username: String, password: String, headFrom: String) {
            database.execBind(
                "INSERT INTO account(id, smtp_server, username, password, header_from)\nVALUES (?, ?, ?, ?, ?)",
                arrayOf(System.currentTimeMillis(), smtpServer, username, password, headFrom)
            )
        }

        fun queryAll(): ArrayList<Account> {
            val list = ArrayList<Account>()

            val statement =
                database.compileStatement("SELECT smtp_server, username, password, header_from\nFROM account ")

            val cursor = statement.cursor
            while (cursor.step()) {
                list.add(
                    Account(
                        SmtpTransport(cursor.getText(0), Credential(cursor.getText(1), cursor.getText(2))),
                        cursor.getText(3)
                    )
                )
            }

            statement.release()
            return list
        }

        companion object {
            private lateinit var databaseFile: File
            fun initPath(context: Context) {
                databaseFile = Common.getInternalDatabaseFile(context, "smtp-config.db")
            }
        }
    }

    companion object {
        var currentAccount: Account? = null
        lateinit var database: Database
    }
}