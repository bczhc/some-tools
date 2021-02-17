package pers.zhc.tools.inputmethod

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.wubi_database_edit_activity.*
import kotlinx.android.synthetic.main.wubi_dict_add_view.*
import kotlinx.android.synthetic.main.wubi_dict_add_view.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.sqlite.SQLite3
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList

/**
 * @author bczhc
 */
class WubiDatabaseEditActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wubi_database_edit_activity)
        val wubiDatabaseInfoTV = wubi_code_database_info
        val wubiCodeSHET = wubi_code_shet
        val wubiCandidatesLL = wubi_candidates_ll
        val addBtn = add_btn
        val wubiCodeET = wubiCodeSHET.editText!!

        val localWubiDatabasePath = WubiCodeSettingActivity.getLocalWubiDatabasePath(this)
        val dictDatabase = SQLite3.open(localWubiDatabasePath)

        Thread {
            var totalRow = 0
            for (a in 'a'..'z') {
                val statement = dictDatabase.compileStatement("SELECT COUNT(*) FROM wubi_code_$a")
                statement.stepRow()
                totalRow += statement.cursor.getInt(0)
                statement.release()
            }
            runOnUiThread {
                wubiDatabaseInfoTV.text = getString(R.string.wubi_code_database_info, totalRow)
            }
        }.start()

        val candidateList = ArrayList<String>()
        var wubiCodeStr = ""

        val setCandidateWords = AtomicReference<Runnable>()
        setCandidateWords.set(Runnable {
            wubiCodeStr = wubiCodeET.text.toString()
            var fetchCandidates: Array<String>? = null
            try {
                fetchCandidates = WubiIME.fetchCandidates(dictDatabase, wubiCodeStr)
            } catch (_: Exception) {
                wubiCandidatesLL.removeAllViews()
            }
            if (fetchCandidates != null && fetchCandidates.isNotEmpty()) {
                wubiCandidatesLL.removeAllViews()
                for (i in fetchCandidates.indices) {
                    val tv = TextView(this@WubiDatabaseEditActivity)
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.gravity = Gravity.TOP
                    tv.layoutParams = layoutParams
                    tv.textSize = 20F
                    tv.text = String.format("%d. %s", i + 1, fetchCandidates[i])
                    tv.setOnLongClickListener {
                        val deleteDialog = DialogUtil.createConfirmationAlertDialog(
                            this,
                            { _, _ ->
                                candidateList.removeAt(i)
                                val wordsCombinedStr = getWordsCombinedStr(candidateList)
                                try {
                                    updateWordRecord(dictDatabase, wubiCodeET.text.toString(), wordsCombinedStr)
                                    ToastUtils.show(this, R.string.delete_successfully)
                                } catch (e: Exception) {
                                    ToastUtils.showError(this, R.string.add_failed, e)
                                } finally {
                                    setCandidateWords.get().run()
                                }
                            },
                            { _, _ -> },
                            R.string.whether_to_delete,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            false
                        )
                        deleteDialog.show()
                        return@setOnLongClickListener true
                    }
                    wubiCandidatesLL.addView(tv)
                }
                candidateList.clear()
                fetchCandidates.forEach { candidateList.add(it) }
            }
        })

        wubiCodeET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                setCandidateWords.get().run()
            }
        })

        addBtn.setOnClickListener {
            val inflate = View.inflate(this, R.layout.wubi_dict_add_view, null)
            val indexSHET = inflate.index_shet
            val indexET = indexSHET.editText!!
            val candidateSHET = inflate.candidate_shet
            val candidateET = candidateSHET.editText!!

            DialogUtil.createConfirmationAlertDialog(
                this,
                { _, _ ->
                    try {
                        candidateList.add(Integer.parseInt(indexET.text.toString()) - 1, candidateET.text.toString())
                    } catch (e: Exception) {
                        when (e) {
                            is NumberFormatException, is IndexOutOfBoundsException -> {
                                ToastUtils.show(this, R.string.please_type_correct_value)
                                return@createConfirmationAlertDialog
                            }
                            else -> throw e
                        }
                    }

                    val newWordStr = getWordsCombinedStr(candidateList)

                    try {
                        updateWordRecord(dictDatabase, wubiCodeStr, newWordStr)
                        ToastUtils.show(this, R.string.add_successfully)
                    } catch (e: Exception) {
                        ToastUtils.showError(this, R.string.add_failed, e)
                    } finally {
                        setCandidateWords.get().run()
                    }
                },
                { _, _ -> },
                inflate,
                R.string.new_add,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            ).show()
        }
    }

    private fun updateWordRecord(dictDatabase: SQLite3, wubiCodeStr: String, newWordStr: String) {
        val statement = dictDatabase.compileStatement(
            "UPDATE wubi_code_${wubiCodeStr[0]} SET word = ? WHERE code is ?"
        )
        statement.bindText(1, newWordStr)
        statement.bindText(2, wubiCodeStr)
        statement.step()
        statement.release()
    }

    private fun getWordsCombinedStr(candidateList: List<String>): String {
        val newWordSB = StringBuilder()
        for (i in candidateList.indices) {
            newWordSB.append(candidateList[i])
            if (i != candidateList.size - 1) newWordSB.append('|')
        }
        return newWordSB.toString()
    }
}