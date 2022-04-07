package pers.zhc.tools.utils

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pers.zhc.tools.R

/**
 * @author bczhc
 */

class RecyclerViewUtils {
    companion object {
        fun buildSimpleItem1ListAdapter(
            context: Context,
            data: List<String>,
            selectable: Boolean = true
        ): RecyclerViewArrayAdapter<String> {
            val adapter = RecyclerViewArrayAdapter(
                context,
                data,
                android.R.layout.simple_list_item_1
            ) { view, s ->
                (view as TextView).text = s
                if (selectable) {
                    view.setBackgroundResource(R.drawable.selectable_bg)
                }
            }
            return adapter
        }
    }
}

fun RecyclerView.addDividerLines() {
    this.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
}

fun RecyclerView.setLinearLayoutManager() {
    this.layoutManager = LinearLayoutManager(this.context)
}
