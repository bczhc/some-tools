package pers.zhc.tools.charsplit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.chars_splitter_list_item.view.*
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.R
import pers.zhc.tools.charucd.CharUcdActivity
import pers.zhc.tools.databinding.CharSplitListFragmentBinding
import pers.zhc.tools.databinding.CharsSplitterListItemBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.CodepointIterator
import pers.zhc.tools.utils.setLinearLayoutManager
import pers.zhc.tools.utils.setUpFastScroll

class CodepointFragment: BaseFragment() {
    private lateinit var listAdapter: MyAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = CharSplitListFragmentBinding.inflate(inflater, container, false)

        listAdapter = MyAdapter(inflater.context)

        val recyclerView = bindings.root
        recyclerView.apply {
            setLinearLayoutManager()
            setUpFastScroll(inflater.context)
            adapter = listAdapter
        }

        return bindings.root
    }

    fun updateList(text: String) {
        listAdapter.update(text)
    }

    private class MyAdapter(private val context: Context) : RecyclerView.Adapter<MyAdapter.MyHolder>() {
        private class MyHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val bindings = CharsSplitterListItemBinding.bind(view)
            val ordinalTV = bindings.ordinalTv
            val codepointTV = bindings.codepointTv
            val charTV = bindings.charTv
        }

        private var list: List<Int> = emptyList()

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
            list = CodepointIterator(text).asSequence().toList()
            notifyDataSetChanged()
        }
    }
}