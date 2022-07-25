package pers.zhc.tools.main

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.github_action_download_view.view.*
import org.json.JSONArray
import org.json.JSONException
import pers.zhc.tools.Info
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

            val findCommitHash = {commitInfo: String->
                val firstLine = commitInfo.split(Regex("\\n"))[0]
                val prefix = "commit "
                val i = firstLine.indexOf(prefix)
                firstLine.substring(i + prefix.length)
            }

            val download = { item: GithubActionDownloadItem ->
                val storagePath = Common.getAppMainExternalStoragePath(context)
                val updateDir = File(storagePath, "update")
                updateDir.requireMkdirs()
                val localFile = File(updateDir, "some-tools.apk")

                val url = URL(Info.staticResourceRootURL + "/apks/some-tools/" + findCommitHash(item.commitInfo) + "/some-tools.apk")
                Download.startDownloadWithDialog(context, url, localFile) {
                    // TODO: check file integrity
                    Common.installApk(context, localFile)
                }
            }

            val onItemClicked = { item: GithubActionDownloadItem, upperDialog: Dialog ->
                DialogUtils.createConfirmationAlertDialog(
                    context,
                    { _, _ ->
                        upperDialog.dismiss()
                        download(item)
                    },
                    titleRes = R.string.app_download_confirmation_dialog_title,
                    message = item.commitInfo,
                    width = MATCH_PARENT
                ).show()
            }

            val jsonArray2DataList = { jsonArray: JSONArray ->
                try {
                    val list = ArrayList<GithubActionDownloadItem>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        list.add(
                            GithubActionDownloadItem(
                                jsonObject.getString("commitInfo"),
                                jsonObject.getString("fileSha1")
                            )
                        )
                    }
                    list
                } catch (e: JSONException) {
                    null
                }
            }

            val showDownloadList = { jsonArray: JSONArray ->
                val data = jsonArray2DataList(jsonArray)
                if (data == null) {
                    ToastUtils.show(context, R.string.getting_information_failed)
                } else {
                    val inflate = View.inflate(context, R.layout.github_action_download_view, null)
                    val recyclerView = inflate.recycler_view!!
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    val adapter = GithubActionDownloadListAdapter(context, data)
                    recyclerView.adapter = adapter

                    val dialog = Dialog(context)
                    DialogUtils.setDialogAttr(dialog, width = MATCH_PARENT, height = WRAP_CONTENT)
                    dialog.setContentView(inflate)
                    dialog.show()

                    adapter.setOnItemClickListener { position, _ ->
                        onItemClicked(data[position], dialog)
                    }
                }
            }

            val progressDialog = ProgressDialog(context)
            val progressView = progressDialog.getProgressView()
            progressView.setIsIndeterminateMode(true)
            progressView.setTitle(context.getString(R.string.getting_information))
            DialogUtils.setDialogAttr(progressDialog, width = MATCH_PARENT, height = WRAP_CONTENT)
            progressDialog.show()
            asyncFetchLogJson {
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

        private fun asyncFetchLogJson(f: (jsonArray: JSONArray?) -> Unit) {
            val url = URL(Info.staticResourceRootURL + "/apks/some-tools/log.json")
            Thread {
                val read = url.readText()
                try {
                    f(JSONArray(read))
                } catch (e: Exception) {
                    f(null)
                }
            }.start()
        }
    }
}