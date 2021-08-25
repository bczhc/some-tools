package pers.zhc.tools.main

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.github_action_download_view.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import pers.zhc.tools.Infos
import pers.zhc.tools.R
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ProgressDialog
import pers.zhc.tools.utils.ToastUtils
import java.net.URL

/**
 * @author bczhc
 */
class MainActivity {
    companion object {
        fun showGithubActionDownloadDialog(context: Context) {

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
            val url = URL(Infos.staticResourceRootURL + "/apks/some-tools/log.json")
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