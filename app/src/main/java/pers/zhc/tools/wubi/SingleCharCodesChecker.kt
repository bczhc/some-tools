package pers.zhc.tools.wubi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.wubi_single_char_record_list_view.view.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI.Utf8
import pers.zhc.tools.utils.Common
import java.io.File

/**
 * @author bczhc
 */
class SingleCharCodesChecker {
    private val inverseDictDatabase = WubiInverseDictManager.openDatabase()
    private val records = ArrayList<Record>()
    private val recordDatabase = RecordDatabase.openDatabase()

    init {
        records.addAll(recordDatabase.queryAll())
    }

    fun commit(char: String, code: String) {
        val query = inverseDictDatabase.query(char)
        if (query.isEmpty()) return
        val shortestCode = query.minOfWith({ a: String, b: String ->
            a.length.compareTo(b.length)
        }, {
            it
        })
        if (code.length > shortestCode.length) {
            if (records.find { it.char == char } == null) {
                records.add(Record(char, shortestCode, code))
                Thread {
                    recordDatabase.update(records)
                }.start()
            }
        }
    }

    companion object {
        fun checkIfSingleChar(s: String): Boolean {
            return Utf8.codepointLength(s) == 1
        }
    }

    protected fun finalize() {
        inverseDictDatabase.close()
    }

    class Record(
        val char: String, val shortestCode: String, val inputCode: String
    )

    inner class RecyclerViewAdapter(val context: Context) : Adapter<RecyclerViewAdapter.MyViewHolder>() {
        inner class MyViewHolder(view: View) : ViewHolder(view) {
            val charTV = view.char_tv!!
            val inputCodeTV = view.input_code_tv!!
            val shortestCodeTV = view.shortest_code_tv!!
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
    }

    fun getRecyclerViewAdapter(context: Context) = RecyclerViewAdapter(context)

    fun clear() {
        records.clear()
        recordDatabase.deleteAll()
    }

    // TODO: database resource lifetime
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

        fun update(records: Iterable<Record>) {
            this.deleteAll()
            for (record in records) {
                val hasRecord = database.hasRecord(
                    """SELECT *
FROM record
WHERE char IS ?
  AND shortest_code IS ?
  AND input_code IS ?""",
                    arrayOf(record.char, record.shortestCode, record.inputCode)
                )

                if (!hasRecord) {
                    insert(record)
                }
            }
        }

        fun insert(record: Record) {
            database.execBind(
                "INSERT INTO record (char, shortest_code, input_code) VALUES (?, ?, ?)",
                arrayOf(record.char, record.shortestCode, record.inputCode)
            )
        }

        fun queryAll(): ArrayList<Record> {
            val records = ArrayList<Record>()

            val statement = database.compileStatement("SELECT char, shortest_code, input_code FROM record")
            val cursor = statement.cursor
            while (cursor.step()) {
                val record = Record(cursor.getText(0), cursor.getText(1), cursor.getText(2))
                records.add(record)
            }
            statement.release()

            return records
        }

        fun deleteAll() {
            @Suppress("SqlWithoutWhere")
            database.exec("DELETE FROM record")
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
}