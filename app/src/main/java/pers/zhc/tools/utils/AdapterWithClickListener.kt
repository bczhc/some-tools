package pers.zhc.tools.utils

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * @author bczhc
 */
abstract class AdapterWithClickListener<T : RecyclerView.ViewHolder?> : RecyclerView.Adapter<T>() {
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
        this.onItemLongClickListener = listener
    }

    fun getOnItemClickListener(): OnItemClickListener? {
        return onItemClickListener
    }

    fun getOnItemLongClickListener(): OnItemLongClickListener? {
        return onItemLongClickListener
    }

    abstract fun onCreateViewHolder(parent: ViewGroup): T

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T & Any {
        val holder = onCreateViewHolder(parent)!!
        val view = holder.itemView
        if (onItemClickListener != null) {
            view.setOnClickListener {
                onItemClickListener?.invoke(holder.layoutPosition, view)
            }
        }
        if (onItemLongClickListener != null) {
            view.setOnLongClickListener {
                onItemLongClickListener?.invoke(holder.layoutPosition, view)
                return@setOnLongClickListener true
            }
        }
        return holder
    }

}

typealias OnItemClickListener = (position: Int, view: View) -> Unit

typealias OnItemLongClickListener = (position: Int, view: View) -> Unit
