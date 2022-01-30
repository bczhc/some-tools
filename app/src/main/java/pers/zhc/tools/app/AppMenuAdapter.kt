package pers.zhc.tools.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.annotations.Contract
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class AppMenuAdapter(private val context: Context, private val activities: ActivityItemData) :
    RecyclerView.Adapter<AppMenuAdapter.MyViewHolder>() {

    @Contract(pure = true)
    fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.main_activity_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val view = holder.itemView
        val tv = view.findViewById<TextView>(R.id.tv)
        val activity = activities[position]
        tv.setText(activity.textRes)

        view.setOnClickListener {
            context.startActivity(Intent(context, activity.activityClass))
        }
        view.setOnLongClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                ToastUtils.show(context, R.string.shortcut_unsupported)
                return@setOnLongClickListener true
            }

            val sm = context.applicationContext.getSystemService(ShortcutManager::class.java).let {
                if (it == null) {
                    ToastUtils.show(context, R.string.deleted_shortcut)
                    return@setOnLongClickListener true
                }
                it
            }
            val dynamicShortcut = sm.dynamicShortcuts


            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return activities.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return onCreateViewHolder(parent)
    }
}

typealias ActivityItemData = List<ActivityItem>