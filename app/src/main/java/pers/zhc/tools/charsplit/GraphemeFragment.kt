package pers.zhc.tools.charsplit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharSplitGraphemeListItemBinding
import pers.zhc.tools.databinding.CharSplitListFragmentBinding
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.GraphemeIterator
import pers.zhc.tools.utils.setLinearLayoutManager
import pers.zhc.tools.utils.setUpFastScroll

class GraphemeFragment : BaseFragment() {
    private lateinit var listAdapter: ListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = CharSplitListFragmentBinding.inflate(inflater, container, false)

        listAdapter = ListAdapter(inflater.context)

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

    class ListAdapter(private val context: Context) : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var graphemes: List<String> = emptyList()

        class MyViewHolder(view: View) : ViewHolder(view) {
            private val bindings = CharSplitGraphemeListItemBinding.bind(view)
            val ordinalTV = bindings.ordinalTv
            val graphemeTV = bindings.graphemeTv
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.char_split_grapheme_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val grapheme = graphemes[position]

            @SuppressLint("SetTextI18n")
            holder.ordinalTV.text = (position + 1).toString()

            holder.graphemeTV.text = grapheme
            holder.graphemeTV.setOnClickListener {
                context.startActivity(Intent(context, CodepointViewActivity::class.java).apply {
                    putExtra(CodepointViewActivity.EXTRA_TEXT, grapheme)
                })
            }
            holder.graphemeTV.setOnLongClickListener {
                ClipboardUtils.putWithToast(context, grapheme)
                true
            }
        }

        override fun getItemCount(): Int {
            return graphemes.size
        }

        @SuppressLint("NotifyDataSetChanged")
        fun update(text: String) {
            graphemes = GraphemeIterator(text).asSequence().toList()
            notifyDataSetChanged()
        }
    }
}