package pers.zhc.tools.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.jetbrains.annotations.Contract
import pers.zhc.tools.R
import pers.zhc.tools.databinding.AppListLongClickedViewBinding
import pers.zhc.tools.utils.ShortcutUtils
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
            onLongClicked(position)
            true
        }
    }

    private fun onLongClicked(position: Int) {
        val bindings = AppListLongClickedViewBinding.inflate(LayoutInflater.from(context), null, false)

        MaterialAlertDialogBuilder(context)
            .setView(bindings.root)
            .show()

        val item = activities[position]
        bindings.appLabelTv.text = context.getString(item.textRes)
        bindings.appClassPathTv.text = item.activityClass.canonicalName
        bindings.toggleAppShortcut.setOnClickListener {
            toggleDynamicAppShortcut(position)
        }
        bindings.createPinnedShortcut.setOnClickListener {
            createPinnedShortcut(position)
        }
    }

    private fun toggleDynamicAppShortcut(position: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            ToastUtils.show(context, R.string.shortcut_unsupported)
            return
        }

        val sm = context.applicationContext.getSystemService(ShortcutManager::class.java).let {
            if (it == null) {
                ToastUtils.show(context, R.string.deleted_shortcut)
                return
            }
            it
        }
        val dynamicShortcuts = sm.dynamicShortcuts

        val setShortcuts = { shortcuts: List<ShortcutInfo> ->
            // rebuild all, because the loss of icons
            val newDynamicShortcuts = shortcuts.map {
                ShortcutInfo.Builder(context, it.id).apply {
                    setIntent(it.intent!!)
                    setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
                    setShortLabel(it.shortLabel!!)
                    setLongLabel(it.longLabel!!)
                }.build()
            }.toList()

            sm.dynamicShortcuts = newDynamicShortcuts
        }

        val activityClass = activities[position].activityClass
        val activityText = context.getString(activities[position].textRes)

        // if exists, remove it (a toggle)
        val removed = dynamicShortcuts.removeIf {
            it.id == activityClass.canonicalName!!
        }
        if (removed) {
            setShortcuts(dynamicShortcuts)
            ToastUtils.show(context, R.string.shortcut_deletion_msg)
            return
        }

        val shortcutSize = dynamicShortcuts.size
        if (shortcutSize + 1 > sm.maxShortcutCountPerActivity) {
            ToastUtils.show(context, R.string.over_quantity_limit)
            return
        }

        val shortcut = ShortcutInfo.Builder(context, activityClass.canonicalName!!).apply {
            setIntent(Intent().apply {
                setClass(context, activityClass)
                action = Intent.ACTION_VIEW
            })
            setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
            setShortLabel(activityText)
            setLongLabel(activityText)
        }.build()

        dynamicShortcuts.add(shortcut)
        setShortcuts(dynamicShortcuts)
        ToastUtils.show(context, R.string.shortcut_create_success_toast)
    }

    private fun createPinnedShortcut(position: Int) {
        ShortcutUtils.checkAndToastPinShortcutsSupported(context)
        val item = activities[position]
        ShortcutUtils.createStartingActivityPinShortcut(context, item.activityClass, context.getString(item.textRes))
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
