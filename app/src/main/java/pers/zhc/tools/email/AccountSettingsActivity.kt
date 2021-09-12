package pers.zhc.tools.email

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import kotlinx.android.synthetic.main.email_account_add_dialog.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.SharedRef

/**
 * @author bczhc
 */
class AccountSettingsActivity : BaseActivity() {
    private lateinit var databaseRef: SharedRef.Ref<Database>
    private lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseRef = Database.getDatabaseRef()
        database = databaseRef.get()

        // TODO: list existing accounts
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.email_account_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_account -> {
                buildAddAccountDialog().show()
            }
            else -> {
            }
        }
        return true
    }

    private fun buildAddAccountDialog(): Dialog {
        val inflate = View.inflate(this, R.layout.email_account_add_dialog, null)
        val smtpServerET = inflate.smtp_server_et!!.editText
        val usernameET = inflate.username_et!!.editText
        val passwordET = inflate.password_et!!.editText
        val fromET = inflate.from_et!!.editText

        val confirmAction = {
            val account = Account(
                SmtpTransport(
                    smtpServerET.text.toString(),
                    Credential(usernameET.text.toString(), passwordET.text.toString())
                ), fromET.text.toString()
            )
            EmailMainActivity.currentAccount = account
            database.insert(account)
        }

        return DialogUtils.createConfirmationAlertDialog(
            this,
            { _, _ ->
                confirmAction()
            },
            view = inflate,
            titleRes = R.string.email_add_account_dialog_title,
            width = MATCH_PARENT
        )
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}