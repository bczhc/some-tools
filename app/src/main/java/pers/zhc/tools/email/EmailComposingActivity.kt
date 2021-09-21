package pers.zhc.tools.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.android.synthetic.main.email_composing_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.email.EmailMainActivity.Companion.currentAccount
import pers.zhc.tools.utils.ProgressDialog
import pers.zhc.tools.utils.SharedRef
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class EmailComposingActivity : BaseActivity() {
    private lateinit var databaseRef: SharedRef.Ref<Database>
    private lateinit var database: Database

    private lateinit var toET: EditText
    private lateinit var ccET: EditText

    enum class ContactAddTarget {
        TO,
        CC
    }

    class ContactResult(val contact: Contact, val target: ContactAddTarget)

    private val contactSelectorLauncher =
        registerForActivityResult(object : ActivityResultContract<ContactAddTarget, ContactResult?>() {
            private lateinit var target: ContactAddTarget

            override fun createIntent(context: Context, input: ContactAddTarget): Intent {
                target = input
                return Intent(context, ContactActivity::class.java).apply {
                    putExtra(ContactActivity.EXTRA_SELECT_MODE, true)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): ContactResult? {
                intent ?: return null
                return ContactResult(intent.getParcelableExtra(ContactActivity.EXTRA_SELECTED_RESULT), target)
            }
        }) {
            it ?: return@registerForActivityResult
            addContact(it.contact, it.target)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseRef = Database.getDatabaseRef()
        database = databaseRef.get()

        setContentView(R.layout.email_composing_activity)

        val subjectET = subject_et!!.editText
        val bodyET = body_et!!.editText
        toET = to_et!!.editText
        ccET = cc_et!!.editText
        val sendButton = send_button!!
        val toAddButton = to_add_btn!!
        val ccAddButton = cc_add_btn!!

        toAddButton.setOnClickListener {
            contactSelectorLauncher.launch(ContactAddTarget.TO)
        }
        ccAddButton.setOnClickListener {
            contactSelectorLauncher.launch(ContactAddTarget.CC)
        }

        sendButton.setOnClickListener {
            // TODO: multi-to and multi-cc
            val message = Message(arrayOf(toET.text.toString()), subjectET.text.toString()).apply {
                ccET.text.toString().let {
                    if (it.isNotEmpty()) {
                        this.cc = arrayOf(it)
                    }
                }
                body = bodyET.text.toString()
            }
            val progressDialog = ProgressDialog(this)
            val progressView = progressDialog.getProgressView()
            progressView.setIsIndeterminateMode(true)
            progressView.setTitle(getString(R.string.email_sending_msg))
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
            Thread {
                try {
                    Sender.send(currentAccount!!, message)
                    runOnUiThread {
                        progressDialog.dismiss()
                        ToastUtils.show(this, R.string.email_send_done_toast)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        ToastUtils.showError(this, R.string.email_send_failed, e)
                    }
                }
            }.start()
        }
    }

    private fun addContact(contact: Contact, target: ContactAddTarget) {
        val add = { c: Contact, et: EditText ->
            et.text.append(" ${c.email}")
        }

        when (target) {
            ContactAddTarget.TO -> add(contact, toET)
            ContactAddTarget.CC -> add(contact, ccET)
        }
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}