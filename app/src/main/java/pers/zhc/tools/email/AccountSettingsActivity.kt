package pers.zhc.tools.email

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.EmailAccountAddDialogBinding
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
        val bindings = EmailAccountAddDialogBinding.inflate(layoutInflater)
        val smtpServerET = bindings.smtpServerEt.editText
        val usernameET = bindings.usernameEt.editText
        val passwordET = bindings.passwordEt.editText
        val fromET = bindings.fromEt.editText

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
            view = bindings.root,
            titleRes = R.string.email_add_account_dialog_title,
            width = MATCH_PARENT
        )
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}
