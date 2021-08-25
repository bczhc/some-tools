package pers.zhc.tools.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.github_action_download_item.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.AdapterWithClickListener

/**
 * @author bczhc
 */
class GithubActionDownloadListAdapter(private val context: Context, private val data: List<GithubActionDownloadItem>) :
    AdapterWithClickListener<GithubActionDownloadListAdapter.MyViewHolder>() {
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commitInfoTV = view.commit_info_tv!!
    }

    override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
        val inflate = LayoutInflater.from(context).inflate(R.layout.github_action_download_item, parent, false)
        return MyViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val (commitInfo, _) = data[position]
        holder.commitInfoTV.text = commitInfo
    }

    override fun getItemCount(): Int {
        return data.size
    }
}