package pers.zhc.tools.inputmethod

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.wubi_database_edit_activity.*
import kotlinx.android.synthetic.main.wubi_dict_add_view.view.*
import kotlinx.android.synthetic.main.wubi_word_with_ordinal_view.view.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.views.WrapLayout
import java.util.*

/**
 * @author bczhc
 */
class WubiDatabaseEditActivity : BaseActivity() {
    private lateinit var wubiCodeET: EditText
    private lateinit var myAdapter: MyAdapter
    private lateinit var dictDatabase: DictionaryDatabase
    private val candidateList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wubi_database_edit_activity)

        val wubiDatabaseInfoTV = wubi_code_database_info
        wubiCodeET = wubi_code_shet!!.editText
        val addBtn = add_btn
        val recyclerView = recycler_view!!
        myAdapter = MyAdapter(this, candidateList)
        recyclerView.adapter = myAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        ItemTouchHelper(TouchHelperCallback(this)).attachToRecyclerView(recyclerView)
        dictDatabase = DictionaryDatabase.getDatabaseRef()

        Thread {
            var totalRow = 0
            for (a in 'a'..'z') {
                val statement = dictDatabase.database.compileStatement("SELECT COUNT(*) FROM wubi_code_$a")
                statement.stepRow()
                totalRow += statement.cursor.getInt(0)
                statement.release()
            }
            runOnUiThread {
                wubiDatabaseInfoTV.text = getString(R.string.wubi_code_database_info, totalRow)
            }
        }.start()

        wubiCodeET.doAfterTextChanged {
            refreshList()
        }

        addBtn.setOnClickListener {
            val inflate = View.inflate(this, R.layout.wubi_dict_add_view, null)
            val wubiWordET = inflate.wubi_word_et!!.editText

            DialogUtils.createConfirmationAlertDialog(
                this,
                { _, _ ->
                    try {
                        dictDatabase.addRecord(wubiWordET.text.toString(), wubiCodeET.text.toString())
                        ToastUtils.show(this, R.string.adding_succeeded)
                    } catch (_: IllegalArgumentException) {
                        ToastUtils.show(this, R.string.please_enter_correct_wubi_code)
                    }
                    refreshList()
                },
                view = inflate,
                titleRes = R.string.add_new,
                width = ViewGroup.LayoutParams.MATCH_PARENT
            ).show()
        }
    }

    private fun refreshList() {
        candidateList.clear()
        try {
            fetchAndSetCandidates()
        } catch (_: Exception) {
        }
        myAdapter.notifyDataSetChanged()
    }

    private fun fetchAndSetCandidates() {
        fetchAndSetCandidates(wubiCodeET.text.toString())
    }

    private fun fetchAndSetCandidates(code: String) {
        val candidates = dictDatabase.fetchCandidates(code)
        candidates ?: candidateList.clear()
        candidates?.forEach {
            candidateList.add(it)
        }
    }

    /**
     * Update wubi word record.
     *
     * If no such column found, it'll create a new record.
     *
     * When after deleting and there's no candidates left, it'll delete the column.
     */
    private fun updateWordRecord(dictDatabase: SQLite3, wubiCodeStr: String, newWordStr: String) {
        val hasRecord = dictDatabase.hasRecord("SELECT * FROM wubi_code_${wubiCodeStr[0]} WHERE code is '$wubiCodeStr'")
        if (hasRecord) {
            // update record
            val statement = dictDatabase.compileStatement(
                "UPDATE wubi_code_${wubiCodeStr[0]} SET word = ? WHERE code is ?"
            )
            statement.bindText(1, newWordStr)
            statement.bindText(2, wubiCodeStr)
            statement.step()
            statement.release()
        } else {
            // add new record
            dictDatabase.exec("INSERT INTO wubi_code_${wubiCodeStr[0]} VALUES('$wubiCodeStr', '$newWordStr')")
        }

        if (!newWordStr.matches(Regex("\\|"))) {
            // have deleted the last left word
            dictDatabase.exec("DELETE FROM wubi_code_${wubiCodeStr[0]} WHERE code is '$wubiCodeStr'")
        }
    }

    private fun getWordsCombinedStr(candidateList: List<String>): String {
        val newWordSB = StringBuilder()
        for (i in candidateList.indices) {
            newWordSB.append(candidateList[i])
            if (i != candidateList.size - 1) newWordSB.append('|')
        }
        return newWordSB.toString()
    }

    class WubiWordView : WrapLayout {
        private var ordinalTV: TextView
        private var wubiWordTV: TextView

        constructor(context: Context?) : this(context, null)

        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
            val inflate = View.inflate(context, R.layout.wubi_word_with_ordinal_view, null)
            this.addView(inflate)

            ordinalTV = inflate.ordinal_tv!!
            wubiWordTV = inflate.wubi_word_tv!!
        }

        @SuppressLint("SetTextI18n")
        fun setOrdinal(i: Int) {
            ordinalTV.text = "$i. "
        }

        fun setWord(word: String) {
            wubiWordTV.text = word
        }
    }


    class MyAdapter(
        private val context: Context,
        private val candidates: ArrayList<String>
    ) : RecyclerView.Adapter<MyAdapter.MyHolder>() {
        class MyHolder(val wordView: WubiWordView) : RecyclerView.ViewHolder(wordView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(WubiWordView(context))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.wordView.setOrdinal(position + 1)
            holder.wordView.setWord(candidates[position])
        }

        override fun getItemCount(): Int {
            return candidates.size
        }
    }

    class TouchHelperCallback(
        private val outer: WubiDatabaseEditActivity
    ) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(
                ItemTouchHelper.UP.xor(ItemTouchHelper.DOWN),
                ItemTouchHelper.LEFT.xor(ItemTouchHelper.RIGHT)
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromIndex = viewHolder.layoutPosition
            val toIndex = target.layoutPosition
            Collections.swap(outer.candidateList, fromIndex, toIndex)
            outer.myAdapter.notifyItemMoved(fromIndex, toIndex)

            outer.dictDatabase.updateRecord(outer.candidateList, outer.wubiCodeET.text.toString())
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val index = viewHolder.layoutPosition
            outer.candidateList.removeAt(index)
            outer.myAdapter.notifyItemRemoved(index)

            outer.dictDatabase.updateRecord(outer.candidateList, outer.wubiCodeET.text.toString())
        }
    }
}