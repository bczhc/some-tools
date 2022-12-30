package pers.zhc.tools.charsplit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.chars_splitter_activity.*
import kotlinx.android.synthetic.main.chars_splitter_list_item.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.charucd.CharUcdActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.CodepointIterator

/**
 * @author bczhc
 */
class CharSplitActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chars_splitter_activity)

        val inputET = input_et!!
        val listView = recycler_view!!

        listView.layoutManager = LinearLayoutManager(this)
        val listAdapter = MyAdapter(this)
        listView.adapter = listAdapter

        inputET.doAfterTextChanged {
            listAdapter.update(inputET.text.toString())
        }
    }

    private class MyAdapter(private val context: Context) : RecyclerView.Adapter<MyAdapter.MyHolder>() {
        private class MyHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ordinalTV = view.ordinal_tv!!
            val codepointTV = view.codepoint_tv!!
            val charTV = view.char_tv!!
        }

        private val list = ArrayList<Int>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(LayoutInflater.from(context).inflate(R.layout.chars_splitter_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val codepoint = list[position]
            val char = JNI.Unicode.Codepoint.codepoint2str(codepoint)

            @SuppressLint("SetTextI18n")
            holder.ordinalTV.text = (position + 1).toString()
            holder.codepointTV.text = UnicodeTable.codepoint2unicodeStr(codepoint)
            holder.charTV.text = char

            holder.charTV.setOnLongClickListener {
                ClipboardUtils.putWithToast(context, char)
                return@setOnLongClickListener true
            }
            holder.charTV.setOnClickListener {
                val intent = Intent(context, CharUcdActivity::class.java).apply {
                    putExtra(CharUcdActivity.EXTRA_CODEPOINT, codepoint)
                }
                context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        @SuppressLint("NotifyDataSetChanged")
        fun update(text: String) {
            list.clear()
            val iter = CodepointIterator(text)
            iter.forEach { list.add(it) }

            notifyDataSetChanged()
        }
    }
}