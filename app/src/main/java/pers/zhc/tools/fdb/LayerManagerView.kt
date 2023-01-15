package pers.zhc.tools.fdb

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import pers.zhc.tools.R
import pers.zhc.tools.databinding.FdbLayerItemViewBinding
import pers.zhc.tools.databinding.FdbLayerManagerViewBinding
import pers.zhc.tools.utils.DialogUtils
import java.util.*

/**
 * @author bczhc
 */
@SuppressLint("ViewConstructor")
class LayerManagerView(context: Context, private val onLayerAddedCallback: OnLayerAddedCallback) :
    CoordinatorLayout(context) {
    private var listAdapter: MyAdapter
    private var recyclerView: RecyclerView
    private val listItems = ArrayList<LayerInfo>()

    init {
        val inflate = View.inflate(context, R.layout.fdb_layer_manager_view, null)
        val bindings = FdbLayerManagerViewBinding.bind(inflate)
        recyclerView = bindings.recyclerView

        listAdapter = MyAdapter(context, listItems, this)
        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val createButton = bindings.create
        createButton.setOnClickListener {
            addLayerAction()
        }

        ItemTouchHelper(TouchHelperCallback(listAdapter)).attachToRecyclerView(recyclerView)

        this.addView(inflate)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addLayerAction() {
        DialogUtils.createPromptDialog(context, R.string.fdb_layer_naming_dialog_title, { _, et ->
            val input = et.text.toString()
            val id = Layer.randomId()

            val layerInfo = LayerInfo(id, input, true)
            listItems.add(0, layerInfo)
            listAdapter.notifyItemInserted(0)

            listAdapter.setChecked(layerInfo.id)
            listAdapter.notifyDataSetChanged()

            onLayerAddedCallback(layerInfo)
        }).also { DialogUtils.setDialogAttr(it, overlayWindow = true) }.show()
    }

    class MyAdapter(private val context: Context, val items: ArrayList<LayerInfo>, val outer: LayerManagerView) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private var checkedId: String? = null

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val bindings = FdbLayerItemViewBinding.bind(view)
            val nameTV = bindings.nameTv
            val rootView = bindings.root
            val visibilityIV = bindings.visibilityBtn
            val editIV = bindings.editButton
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.fdb_layer_item_view, parent, false)
            return MyViewHolder(inflate)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val layerInfo = items[position]

            holder.nameTV.text = layerInfo.name
            if (layerInfo.id == checkedId) {
                holder.rootView.setBackgroundResource(R.drawable.view_stroke_red)
            } else {
                holder.rootView.setBackgroundResource(R.drawable.view_stroke)
            }

            holder.itemView.setOnClickListener {
                checkedId = items[holder.layoutPosition].id
                notifyDataSetChanged()
            }

            val updateVisibilityIcon = {
                holder.visibilityIV.setImageResource(
                    if (layerInfo.visible) {
                        R.drawable.ic_visibility
                    } else {
                        R.drawable.ic_visibility_off
                    }
                )
            }.also { it() }

            holder.visibilityIV.setOnClickListener {
                // toggle visibility
                layerInfo.visible = !layerInfo.visible
                updateVisibilityIcon()
            }

            holder.editIV.setOnClickListener {
                // change layer name
                val editText = TextInputEditText(context)
                    .apply {
                        setText(layerInfo.name)
                    }
                DialogUtils.createPromptDialog(
                    context, R.string.fdb_layer_naming_dialog_title,
                    positiveAction = { _, et ->
                        val newName = et.text.toString()
                        layerInfo.name = newName
                        notifyItemChanged(position)
                    },
                    editText = editText
                ).apply {
                    DialogUtils.setDialogAttr(this, width = ViewGroup.LayoutParams.MATCH_PARENT, overlayWindow = true)
                }.show()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        fun getCheckedLayerId(): String? {
            return checkedId
        }

        fun setChecked(id: String) {
            this.checkedId = id
        }
    }

    class TouchHelperCallback(
        private val listAdapter: MyAdapter,
    ) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            var swipeFlag = ItemTouchHelper.LEFT
            // if the item is checked, avoid being swiped away
            if (listAdapter.getCheckedLayerId() == listAdapter.items[viewHolder.layoutPosition].id) {
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
            val removed = listAdapter.items.removeAt(index)
            listAdapter.notifyItemRemoved(index)

            showDeletedSnackbar(index, removed)
        }

        private fun showDeletedSnackbar(index: Int, removed: LayerInfo) {
            val snackBar = Snackbar.make(listAdapter.outer, R.string.deleted_message, Snackbar.LENGTH_LONG).apply {
                setAction(R.string.undo) {
                    listAdapter.items.add(index, removed)
                    listAdapter.notifyItemInserted(index)
                }
            }
            snackBar.show()
        }
    }

    fun add1Layer(layerInfo: LayerInfo) {
        listItems.add(0, layerInfo)
        listAdapter.notifyItemInserted(0)
    }

    fun getLayerState(): LayerState {
        return LayerState(listAdapter.items, listAdapter.getCheckedLayerId())
    }

    class LayerState(
        val orderList: List<LayerInfo>,
        val checkedId: String?
    )

    fun setChecked(id: String) {
        listAdapter.setChecked(id)
    }

    fun getLayersInfo(): ArrayList<LayerInfo> {
        return listItems
    }
}

typealias OnLayerAddedCallback = (layerInfo: LayerInfo) -> Unit