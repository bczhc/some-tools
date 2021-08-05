package pers.zhc.tools.fdb

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fdb_layer_item_view.view.*
import kotlinx.android.synthetic.main.fdb_layer_manager_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import java.util.*

/**
 * @author bczhc
 */
@SuppressLint("ViewConstructor")
class LayerManagerView(context: Context, private val onLayerAddedCallback: OnLayerAddedCallback) :
    RelativeLayout(context) {
    private var listAdapter: MyAdapter
    private var recyclerView: RecyclerView
    private val listItems = ArrayList<LayerItem>()

    init {
        val inflate = View.inflate(context, R.layout.fdb_layer_manager_view, null)
        recyclerView = inflate.recycler_view!!

        listAdapter = MyAdapter(context, listItems)
        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val createButton = inflate.create!!
        createButton.setOnClickListener {
            addLayerAction()
        }

        ItemTouchHelper(TouchHelperCallback(listAdapter)).attachToRecyclerView(recyclerView)

        this.addView(inflate)
    }

    private fun addLayerAction() {
        DialogUtils.createPromptDialog(context, R.string.fdb_layer_naming_dialog_title, { _, et ->
            val input = et.text.toString()

            val id = System.currentTimeMillis()
            listItems.add(LayerItem(id, input))
            listAdapter.notifyItemInserted(listItems.size)

            onLayerAddedCallback(id)
        }).also { DialogUtils.setDialogAttr(it, overlayWindow = true) }.show()
    }

    class MyAdapter(private val context: Context, val items: ArrayList<LayerItem>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private var checkedId = -1L

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTV = view.name_tv!!
            val ll = view.root_ll!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.fdb_layer_item_view, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.nameTV.text = items[position].name
            if (items[position].layerId == checkedId) {
                holder.ll.setBackgroundResource(R.drawable.view_stroke_red)
            } else {
                holder.ll.setBackgroundResource(R.drawable.view_stroke)
            }

            holder.itemView.setOnClickListener {
                checkedId = items[holder.layoutPosition].layerId
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        fun getIdOrderList(): ArrayList<Long> {
            val idList = ArrayList<Long>()
            items.forEach {
                idList.add(it.layerId)
            }
            return idList
        }

        fun getCheckedLayerId(): Long {
            return checkedId
        }

        fun setChecked(id: Long) {
            this.checkedId = id
        }
    }

    class LayerItem(
        val layerId: Long,
        val name: String
    )

    class TouchHelperCallback(
        private val listAdapter: MyAdapter,
    ) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            var swipeFlag = ItemTouchHelper.LEFT
            // if the item is checked, avoid being swipe away
            if (listAdapter.getCheckedLayerId() == listAdapter.items[viewHolder.layoutPosition].layerId) {
                swipeFlag = 0
            }
            val dragFlag = ItemTouchHelper.UP
                .xor(ItemTouchHelper.DOWN)

            return makeMovementFlags(dragFlag, swipeFlag)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromIndex = viewHolder.layoutPosition
            val toIndex = target.layoutPosition
            Collections.swap(listAdapter.items, fromIndex, toIndex)
            listAdapter.notifyItemMoved(fromIndex, toIndex)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val index = viewHolder.layoutPosition
            listAdapter.items.removeAt(index)
            listAdapter.notifyItemRemoved(index)
        }
    }

    fun add1Layer(id: Long, name: String) {
        listItems.add(LayerItem(id, name))
        listAdapter.notifyItemInserted(listItems.size)
    }

    fun getLayerState(): LayerState {
        return LayerState(listAdapter.getIdOrderList(), listAdapter.getCheckedLayerId())
    }

    class LayerState(
        val orderList: List<Long>,
        val checkedId: Long
    )

    fun setChecked(id: Long) {
        listAdapter.setChecked(id)
    }
}

typealias OnLayerAddedCallback = (id: Long) -> Unit