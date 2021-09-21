package pers.zhc.tools.email

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.json.JSONException
import org.json.JSONObject
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.SharedRef
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.readToString
import java.io.File

/**
 * @author bczhc
 */
class EmailMainActivity : BaseActivity() {
    private lateinit var databaseRef: SharedRef.Ref<Database>
    private lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseRef = Database.getDatabaseRef()
        database = databaseRef.get()

        setupCurrentAccount()

        setContentView(R.layout.email_main_activity)
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
            R.id.compose -> {
                startActivity(Intent(this, EmailComposingActivity::class.java))
            }
            R.id.contact -> {
                startActivity(Intent(this, ContactActivity::class.java))
            }
            else -> {
            }
        }
        return true
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
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

    companion object {
        var currentAccount: Account? = null
    }
}