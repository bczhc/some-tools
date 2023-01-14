package pers.zhc.tools

import android.os.Bundle
import org.json.JSONObject
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_GITHUB_RAW_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_SERVER_ROOT_URL
import pers.zhc.tools.MyApplication.Companion.InfoJson.Companion.KEY_STATIC_RESOURCE_ROOT_URL
import pers.zhc.tools.databinding.MainActivityBinding
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class Settings : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = MainActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val serverET = bindings.serverEt
        val resourceET = bindings.resourceEt
        val githubET = bindings.githubRawUrlEt
        val saveBtn = bindings.save

        serverET.editText.setText(Info.serverRootURL)
        resourceET.editText.setText(Info.staticResourceRootURL)
        githubET.editText.setText(Info.githubRawRootURL)

        saveBtn.setOnClickListener {
            val jsonObject = JSONObject()
            val newServerURL = serverET.editText.text.toString()
            val newResourceURL = resourceET.editText.text.toString()
            val newGithubRawURL = githubET.editText.text.toString()
            jsonObject.put(KEY_SERVER_ROOT_URL, newServerURL)
            jsonObject.put(KEY_STATIC_RESOURCE_ROOT_URL, newResourceURL)
            jsonObject.put(KEY_GITHUB_RAW_ROOT_URL, newGithubRawURL)
            MyApplication.writeInfoJSON(jsonObject)


            Info.serverRootURL = newServerURL
            Info.staticResourceRootURL = newResourceURL
            Info.githubRawRootURL = newGithubRawURL

            ToastUtils.show(this, R.string.saved)
            finish()
        }
    }
}