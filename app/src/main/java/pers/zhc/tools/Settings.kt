package pers.zhc.tools

import android.os.Bundle
import kotlinx.android.synthetic.main.main_activity.*
import org.json.JSONObject
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class Settings : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val serverET = server_et!!
        val resourceET = resource_et!!
        val saveBtn = save!!

        serverET.editText.setText(Infos.serverURL)
        resourceET.editText.setText(Infos.resourceURL)

        saveBtn.setOnClickListener {
            val jsonObject = JSONObject()
            val newServerURL = serverET.editText.text.toString()
            val newResourceURL = resourceET.editText.text.toString()
            jsonObject.put("serverURL", newServerURL)
            jsonObject.put("resourceURL", newResourceURL)
            MyApplication.writeInfoJSON(jsonObject)


            Infos.serverURL = newServerURL
            Infos.resourceURL = newResourceURL

            ToastUtils.show(this, R.string.saved)
            finish()
        }
    }
}