package pers.zhc.tools.charucd

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharNameLookupFragmentBinding
import pers.zhc.tools.databinding.CharUcdNameLookupListItemBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*

class CharNameLookupFragment : BaseFragment() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = inflater.context
        val bindings = CharNameLookupFragmentBinding.inflate(inflater, container, false)

        val recyclerView = bindings.recyclerView
        val charNameET = bindings.charNameEt
        val hintTV = bindings.resultHintTv

        val listItems: ListItems = mutableListOf()

        val listAdapter = ListAdapter(listItems)
        recyclerView.apply {
            setLinearLayoutManager()
            adapter = listAdapter
            setUpFastScroll(requireContext())
        }

        val queryResult = {
            val lookupName = charNameET.text.toString()
            UcdDatabase.useDatabase {
                it.queryByNameLike(lookupName, LOOKUP_LIMIT)
            }
        }

        // process and prepare the data for RecyclerView
        val processQueryResult = { result: UcdDatabase.NameLookupResult ->
            result.result.map {
                val displayName = buildList {
                    if (it.alias == null) {
                        listOf(it.na, it.na1)
                    } else {
                        listOf(it.na, it.na1, it.alias)
                    }.forEach { ucdName ->
                        if (ucdName.contains(result.lookupName, ignoreCase = true)) {
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
        }

        listAdapter.setOnItemClickListener { position, _ ->
            val codepoint = listItems[position].codepoint
            startActivity(Intent(context, CharUcdActivity::class.java).apply {
                putExtra(CharUcdActivity.EXTRA_CODEPOINT, codepoint)
            })
        }
        listAdapter.setOnItemLongClickListener { position, _ ->
            ClipboardUtils.putWithToast(context, listItems[position].char)
        }

        val debounceInterval = 300L
        var job: Job? = null
        charNameET.doAfterTextChanged {
            job?.cancel()
            job = viewLifecycleOwner.lifecycleScope.launch {
                delay(debounceInterval)
                hintTV.setText(R.string.char_ucd_name_lookup_hint_tv_querying)
                thread {
                    val queried = queryResult()
                    val processed = processQueryResult(queried)
                    context.runOnUiThread {
                        hintTV.text =
                            getString(R.string.char_ucd_name_lookup_hint_tv_result, queried.totalCount, LOOKUP_LIMIT)
                        listItems.clear()
                        listItems.addAll(processed)
                        listAdapter.notifyDataSetChanged()
                    }
                }
            }
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

    data class CharData(
        val codepoint: Int,
        val char: String,
        val name: String,
    )
}

typealias ListItems = MutableList<CharNameLookupFragment.CharData>
