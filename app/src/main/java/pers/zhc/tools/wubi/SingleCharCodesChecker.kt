package pers.zhc.tools.wubi

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.R
import pers.zhc.tools.databinding.WubiSingleCharRecordListViewBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.execute
import java.io.File
import java.util.concurrent.Executors

/**
 * @author bczhc
 */
class SingleCharCodesChecker(
    private val notifyCallback: (record: Record) -> Unit
) {
    private val inverseDictDatabase = WubiInverseDictManager.openDatabase()
    private val recordDatabase = RecordDatabase.openDatabase()
    private val threadPool = Executors.newFixedThreadPool(1)

    fun asyncCommit(char: String, code: String, rule: CheckingRule) {
        threadPool.execute {
            val query = inverseDictDatabase.query(char)
            if (query.isEmpty()) return@execute
            if (rule.test(code, query)) {
                val record = Record(char, getShortest(query), code)
                notifyCallback(record)
                if (!recordDatabase.exist(record)) {
                    recordDatabase.insert(record)
                }
            }
        }
    }

    companion object {
        fun checkIfSingleChar(s: String): Boolean {
            return JNI.Unicode.Codepoint.codepointLength(s) == 1
        }

        fun getShortest(codes: Array<String>) = codes.minOfWith({ a: String, b: String ->
            a.length.compareTo(b.length)
        }, {
            it
        })
    }

    protected fun finalize() {
        inverseDictDatabase.close()
        threadPool.shutdown()
    }

    class Record(
        val char: String, val shortestCode: String, val inputCode: String
    )

    fun clear() {
        recordDatabase.deleteAll()
    }

    fun getRecyclerViewAdapter(context: Context): RecyclerViewAdapter {
        return object : RecyclerViewAdapter(context) {
            override fun onQuery(): List<Record> {
                return recordDatabase.queryAll()
            }
        }
    }

    fun queryAll(): ArrayList<Record> {
        return recordDatabase.queryAll()
    }

    // TODO: database lifetime and resources releasing
    class RecordDatabase private constructor(val database: SQLite3) {

        init {
            database.exec(
                """CREATE TABLE IF NOT EXISTS record
(
    char          TEXT NOT NULL,
    shortest_code TEXT NOT NULL,
    input_code    TEXT NOT NULL
)"""
            )
        }

        private val existenceStatement = database.compileStatement(
            """SELECT *
FROM record
WHERE char IS ?
  AND shortest_code IS ?
  AND input_code IS ?"""
        )

        private val insertStatement =
            database.compileStatement("""INSERT INTO record (char, shortest_code, input_code) VALUES (?, ?, ?)""")

        private val selectAllStatement = database.compileStatement("SELECT char, shortest_code, input_code FROM record")

        fun exist(record: Record): Boolean {
            existenceStatement.reset()
            existenceStatement.bind(getFullBinds(record))
            return existenceStatement.cursor.step()
        }

        fun insert(record: Record) {
            insertStatement.execute(getFullBinds(record))
        }

        private fun getFullBinds(record: Record): Array<Any> {
            return arrayOf(record.char, record.shortestCode, record.inputCode)
        }

        fun queryAll(): ArrayList<Record> {
            val records = ArrayList<Record>()

            selectAllStatement.reset()
            val cursor = selectAllStatement.cursor
            while (cursor.step()) {
                val record = Record(cursor.getText(0), cursor.getText(1), cursor.getText(2))
                records.add(record)
            }

            return records
        }

        fun deleteAll() {
            @Suppress("SqlWithoutWhere") database.exec("DELETE FROM record")
        }

        companion object {
            private lateinit var path: File

            fun init(context: Context) {
                path = Common.getInternalDatabaseFile(context, "wubi-single-char-records.db")
            }

            private val database by lazy {
                SQLite3.open(path.path)
            }

            fun openDatabase(): RecordDatabase {
                return RecordDatabase(database)
            }
        }
    }

    abstract class RecyclerViewAdapter(val context: Context) : Adapter<RecyclerViewAdapter.MyViewHolder>() {
        private val records = ArrayList<Record>().apply {
            addAll(onQuery())
        }

        inner class MyViewHolder(view: View) : ViewHolder(view) {
            private val bindings = WubiSingleCharRecordListViewBinding.bind(view)
            val charTV = bindings.charTv
            val inputCodeTV = bindings.inputCodeTv
            val shortestCodeTV = bindings.shortestCodeTv
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate =
                LayoutInflater.from(context).inflate(R.layout.wubi_single_char_record_list_view, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val record = records[position]
            holder.charTV.text = record.char
            holder.inputCodeTV.text =
                context.getString(R.string.wubi_single_char_code_check_input_code_tv, record.inputCode)
            holder.shortestCodeTV.text =
                context.getString(R.string.wubi_single_char_code_check_shortest_code_tv, record.shortestCode)
        }

        override fun getItemCount(): Int {
            return records.size
        }

        protected abstract fun onQuery(): List<Record>

        @SuppressLint("NotifyDataSetChanged")
        fun updateList() {
            records.clear()
            records.addAll(onQuery())
            notifyDataSetChanged()
        }
    }

    enum class CheckingRule(val test: (code: String, inverseCode: Array<String>) -> Boolean) {
        SHORTCUT_1_2_3({ code, inverseCode ->
            val shortestCode = getShortest(inverseCode)
            code.length > shortestCode.length && shortestCode.length in 1..3
        }),

        SHORTCUT_1_2({ code, inverseCode ->
            val shortestCode = getShortest(inverseCode)
            code.length > shortestCode.length && shortestCode.length in 1..2
        });
    }
}