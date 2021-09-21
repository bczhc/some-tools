package pers.zhc.tools.app

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.annotations.Contract
import pers.zhc.tools.R

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