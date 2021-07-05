package pers.zhc.tools.test

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.unicode_cell_view.view.*
import kotlinx.android.synthetic.main.unicode_table_activity.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import java.util.*

/**
 * @author bczhc
 */
class UnicodeTable : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unicode_table_activity)

        val recyclerView = recycler_view!!

        recyclerView.layoutManager = GridLayoutManager(this, 6)
        recyclerView.adapter = MyAdapter(this)

        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(this@UnicodeTable, R.drawable.thumb)!!)
        }.build()
    }

    class MyAdapter(private val ctx: Context) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
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
        }

        override fun getItemCount(): Int {
            return 0x10FFFF
        }
    }
}