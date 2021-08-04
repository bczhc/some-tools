package pers.zhc.tools.fdb

import android.content.Context
import android.util.AttributeSet
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
import kotlin.collections.ArrayList

/**
 * @author bczhc
 */
class LayerManagerView : RelativeLayout {
    private lateinit var listAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView
    private val listItems = ArrayList<LayerItem>()

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
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
            listItems.add(LayerItem(Layer(10, 10), input))
            listAdapter.notifyItemInserted(listItems.size)
        }).also { DialogUtils.setDialogAttr(it, overlayWindow = true) }.show()
    }

    class MyAdapter(private val context: Context, val items: ArrayList<LayerItem>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var nameTV: TextView = view.name_tv!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.fdb_layer_item_view, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.nameTV.text = items[position].name
        }

        override fun getItemCount(): Int {
            return items.size
        }

    }

    class LayerItem(
        val layer: Layer,
        val name: String
    )

    class TouchHelperCallback(private val listAdapter: MyAdapter) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlag = ItemTouchHelper.UP
                .xor(ItemTouchHelper.DOWN)
            val swipeFlag = ItemTouchHelper.LEFT

            return makeMovementFlags(dragFlag, swipeFlag)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromIndex = viewHolder.layoutPosition
            val toIndex = target.layoutPosition
            val moved = listAdapter.items[fromIndex]
            Collections.swap(listAdapter.items, fromIndex, toIndex)
            listAdapter.notifyItemMoved(fromIndex, toIndex)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            listAdapter.items.removeAt(viewHolder.layoutPosition)
            listAdapter.notifyItemRemoved(viewHolder.layoutPosition)
        }

    }
}
