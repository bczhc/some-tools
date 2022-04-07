package pers.zhc.tools.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * @author bczhc
 */
class RecyclerViewArrayAdapter<T>(
    val context: Context,
    val data: List<T>,
    @LayoutRes val itemRes: Int,
    val onBindCallback: OnBindCallback<T>
) :
    AdapterWithClickListener<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return object : ViewHolder(LayoutInflater.from(context).inflate(itemRes, parent, false)) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindCallback(holder.itemView, data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

typealias OnBindCallback<T> = (view: View, obj: T) -> Unit