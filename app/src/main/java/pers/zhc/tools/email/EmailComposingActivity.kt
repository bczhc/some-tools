package pers.zhc.tools.email

import android.os.Bundle
import kotlinx.android.synthetic.main.email_composing_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.email.EmailMainActivity.Companion.currentAccount
import pers.zhc.tools.utils.*

/**
 * @author bczhc
 */
class EmailComposingActivity : BaseActivity() {
    private lateinit var databaseRef: SharedRef.Ref<Database>
    private lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseRef = Database.getDatabaseRef()
        database = databaseRef.get()

        setContentView(R.layout.email_composing_activity)

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

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}