package pers.zhc.tools.wubi

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.widget.doAfterTextChanged
import kotlinx.android.synthetic.main.wubi_code_looking_up_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ProgressDialog
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class WubiCodeLookingUpActivity : BaseActivity() {
    private lateinit var dict: WubiInverseDictDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wubi_code_looking_up_activity)

        dict = WubiInverseDictManager.openDatabase()
        val updateNeeded = dict.checkUpdate()
        if (updateNeeded) {
            DialogUtils.createConfirmationAlertDialog(this, { _, _ ->

                showUpdateInverseDict()

            }, titleRes = R.string.wubi_inverse_dict_update_alert).show()
        }

        val wubiWordET = wubi_word_et!!.editText
        val resultTV = result_tv!!

        wubiWordET.doAfterTextChanged {
            val codes = dict.query(wubiWordET.text.toString())
            resultTV.text = codes.joinToString("\n")
        }
    }

    private fun showUpdateInverseDict() {
        val progressDialog = ProgressDialog(this).also {
            val progressView = it.getProgressView()
            progressView.setIsIndeterminateMode(true)
            progressView.setTitle(getString(R.string.updating))
            it.setCanceledOnTouchOutside(false)
            it.setCancelable(false)
        }
        progressDialog.show()

        Thread {
            dict.update(DictionaryDatabase.dictDatabase.database)

            runOnUiThread {
                progressDialog.dismiss()
                ToastUtils.show(this, R.string.process_done)
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wubi_inverse_dict_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.update -> {
                showUpdateInverseDict()
            }
            else -> {}
        }
        return true
    }

    override fun finish() {
        dict.close()
        super.finish()
    }
}