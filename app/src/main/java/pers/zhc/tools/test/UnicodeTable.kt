package pers.zhc.tools.test

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.unicode_cell_view.view.*
import kotlinx.android.synthetic.main.unicode_table_activity.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.utils.PromptDialogCallback
import pers.zhc.tools.utils.ToastUtils
import java.util.*

/**
 * @author bczhc
 */
class UnicodeTable : BaseActivity() {
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unicode_table_activity)

        recyclerView = recycler_view!!

        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        val listWidth = point.x

        val expectedCellWidth = listWidth.toFloat() / 6F

        recyclerView.layoutManager = GridLayoutManager(this, 6)
        // TODO: 7/8/21 set the text in the cells auto-sized
        recyclerView.adapter = MyAdapter(this, /*DisplayUtil.px2sp(this, expectedCellWidth).toFloat()*/ 40F)

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(this@UnicodeTable, R.drawable.thumb)!!)
        }.build()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.unicode_table_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.seek -> {
                showSeekDialog()
            }
            else -> {
            }
        }
        return true
    }

    private fun showSeekDialog() {
        DialogUtil.createPromptDialog(
            this,
            R.string.unicode_table_codepoint_hex_is_dialog,
            PromptDialogCallback { et, alertDialog ->
                val input = et.text.toString()
                try {
                    val codepoint = input.toInt(16)
                    seek(codepoint)
                } catch (e: NumberFormatException) {
                    ToastUtils.showError(this, R.string.please_enter_correct_value_toast, e)
                }
            })
            .show()
    }

    private fun seek(codepoint: Int) {
        recyclerView.scrollToPosition(codepoint)
    }

    class MyAdapter(private val ctx: Context, private val cellTextWidth: Float) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val charTV: TextView = view.char_tv!!
            val codepointTV: TextView = view.codepoint_tv!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflate = LayoutInflater.from(ctx).inflate(R.layout.unicode_cell_view, parent, false)
            return MyViewHolder(inflate)
        }

        private val charBuf = CharArray(2)

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            var s = position.toString(16).toUpperCase(Locale.US)
            if (s.length < 4) s = "0".repeat(4 - s.length) + s
            holder.codepointTV.text = "U+$s"

            val len = Character.toChars(position, charBuf, 0)
            holder.charTV.text = String(charBuf, 0, len)
            holder.charTV.textSize = cellTextWidth

            holder.charTV.setOnLongClickListener {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val cd = ClipData.newPlainText("character", holder.charTV.text)
                cm.setPrimaryClip(cd)
                ToastUtils.show(ctx, R.string.unicode_table_copying_succeeded_toast)
                return@setOnLongClickListener true
            }
        }

        override fun getItemCount(): Int {
            // for 0..0x10FFFF, so the count is 0x10FFFF + 1
            return 0x10FFFF + 1
        }
    }
}