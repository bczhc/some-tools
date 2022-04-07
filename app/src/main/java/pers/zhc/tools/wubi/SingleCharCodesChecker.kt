package pers.zhc.tools.wubi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.wubi_single_char_record_list_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI.Utf8

/**
 * @author bczhc
 */
class SingleCharCodesChecker {
    private val inverseDictDatabase = WubiInverseDictManager.openDatabase()
    private val records = ArrayList<Record>()

    fun commit(char: String, code: String) {
        val query = inverseDictDatabase.query(char)
        val shortestCode = query.minOfWith({ a: String, b: String ->
            a.length.compareTo(b.length)
        }, {
            it
        })
        if (code.length > shortestCode.length) {
            if (records.find { it.char == char } == null) {
                records.add(Record(char, shortestCode, code))
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
        val char: String,
        val shortestCode: String,
        val inputCode: String
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
    }
}