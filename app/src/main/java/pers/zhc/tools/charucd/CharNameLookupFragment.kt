package pers.zhc.tools.charucd

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.databinding.CharNameLookupFragmentBinding
import pers.zhc.tools.databinding.CharUcdNameLookupListItemBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.AdapterWithClickListener
import pers.zhc.tools.utils.setLinearLayoutManager
import pers.zhc.tools.utils.setUpFastScroll

class CharNameLookupFragment : BaseFragment() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = CharNameLookupFragmentBinding.inflate(inflater, container, false)

        val recyclerView = bindings.recyclerView
        val charNameET = bindings.charNameEt
        val lookupButton = bindings.lookupBtn

        val listItems: ListItems = mutableListOf()

        val listAdapter = ListAdapter(listItems)
        recyclerView.apply {
            setLinearLayoutManager()
            adapter = listAdapter
            setUpFastScroll(requireContext())
        }

        lookupButton.setOnClickListener {
            val lookupName = charNameET.text.toString()
            val result = UcdDatabase.useDatabase {
                it.queryByNameLike(lookupName, LOOKUP_LIMIT)
            }.map {
                val displayName = buildList {
                    if (it.alias == null) {
                        listOf(it.na, it.na1)
                    } else {
                        listOf(it.na, it.na1, it.alias)
                    }.forEach { ucdName ->
                        if (ucdName.contains(lookupName, ignoreCase = true)) {
                            this += ucdName
                        }
                    }
                }.joinToString("\n")

                CharData(
                    it.codepoint,
                    JNI.Unicode.Codepoint.codepoint2str(it.codepoint),
                    displayName
                )
            }
            listItems.clear()
            listItems.addAll(result)
            listAdapter.notifyDataSetChanged()
        }

        listAdapter.setOnItemClickListener { position, _ ->
            val codepoint = listItems[position].codepoint
            startActivity(Intent(inflater.context, CharUcdActivity::class.java).apply {
                putExtra(CharUcdActivity.EXTRA_CODEPOINT, codepoint)
            })
        }

        return bindings.root
    }

    private class ListAdapter(private val listItems: ListItems) : AdapterWithClickListener<ListAdapter.MyViewHolder>() {
        private class MyViewHolder(bindings: CharUcdNameLookupListItemBinding) : ViewHolder(bindings.root) {
            val charTV = bindings.charTv
            val nameTV = bindings.nameTv
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = CharUcdNameLookupListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(bindings)
        }

        override fun getItemCount() = listItems.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.charTV.text = listItems[position].char
            holder.nameTV.text = listItems[position].name
        }
    }

    companion object {
        const val LOOKUP_LIMIT = 1000
    }
}

typealias ListItems = MutableList<CharData>

data class CharData(
    val codepoint: Int,
    val char: String,
    val name: String,
)
