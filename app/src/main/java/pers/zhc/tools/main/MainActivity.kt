package pers.zhc.tools.main

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.github_action_download_view.view.*
import pers.zhc.tools.Info
import pers.zhc.tools.MyApplication.Companion.GSON
import pers.zhc.tools.R
import pers.zhc.tools.utils.*
import java.io.File
import java.net.URL

/**
 * @author bczhc
 */
class MainActivity {
    companion object {
        fun showGithubActionDownloadDialog(context: Context) {
            val download = { item: Commit, abi: String ->

                val apk = item.apks.find { it.abi == abi }!!

                val storagePath = Common.getAppMainExternalStoragePath(context)
                val updateDir = File(storagePath, "update")
                updateDir.requireMkdirs()
                val localFile = File(updateDir, "some-tools.apk")

                val url =
                    URL("${Info.staticResourceRootURL}/apks/some-tools/${item.commitHash}/${apk.name}")
                Download.startDownloadWithDialog(context, url, localFile) {
                    // TODO: check file integrity
                    Common.installApk(context, localFile)
                }
            }

            val onItemClicked = { item: Commit, upperDialog: Dialog ->
                // check abi
                val supportedAbis = Build.SUPPORTED_ABIS
                val foundAbi = item.apks.map { it.abi }.find {
                    supportedAbis.contains(it)
                }
                if (foundAbi == null) {
                    ToastUtils.show(context, R.string.app_unsupported_abi)
                } else {
                    DialogUtils.createConfirmationAlertDialog(
                        context,
                        { _, _ ->
                            download(item, foundAbi)
                            upperDialog.dismiss()
                        },
                        titleRes = R.string.app_download_confirmation_dialog_title,
                        message = item.commitMessage,
                        width = MATCH_PARENT
                    ).show()
                }

            }

            val showDownloadList = { infoJson: String ->
                val commits = try {
                    ArrayList(
                        GSON.fromJson(infoJson, JsonArray::class.java).map { GSON.fromJson(it, Commit::class.java) })
                } catch (_: JsonSyntaxException) {
                    null
                }

                if (commits == null) {
                    ToastUtils.show(context, R.string.getting_information_failed)
                } else {
                    val inflate = View.inflate(context, R.layout.github_action_download_view, null)
                    val recyclerView = inflate.recycler_view!!
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    val adapter = GithubActionDownloadListAdapter(context, commits)
                    recyclerView.adapter = adapter

                    val dialog = Dialog(context)
                    DialogUtils.setDialogAttr(dialog, width = MATCH_PARENT, height = WRAP_CONTENT)
                    dialog.setContentView(inflate)
                    dialog.show()

                    adapter.setOnItemClickListener { position, _ ->
                        onItemClicked(commits[position], dialog)
                    }
                }
            }

            val progressDialog = ProgressDialog(context)
            val progressView = progressDialog.getProgressView()
            progressView.setIsIndeterminateMode(true)
            progressView.setTitle(context.getString(R.string.getting_information))
            DialogUtils.setDialogAttr(progressDialog, width = MATCH_PARENT, height = WRAP_CONTENT)
            progressDialog.show()
            asyncFetchInfo {
                Common.runOnUiThread(context) {
                    progressDialog.dismiss()
                    if (it == null) {
                        ToastUtils.show(context, R.string.getting_information_failed)
                        return@runOnUiThread
                    }
                    showDownloadList(it)
                }
            }
        }

        private fun asyncFetchInfo(f: (read: String?) -> Unit) {
            val url = URL(Info.staticResourceRootURL + "/apks/some-tools/files.json")
            Thread {
                val read = url.readText()
                try {
                    f(read)
                } catch (e: Exception) {
                    f(null)
                }
            }.start()
        }
    }
}

typealias Commits = ArrayList<Commit>