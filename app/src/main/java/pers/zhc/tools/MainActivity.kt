package pers.zhc.tools

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject
import pers.zhc.tools.clipboard.Clip
import pers.zhc.tools.codecs.CodecsActivity
import pers.zhc.tools.crashhandler.CrashTest
import pers.zhc.tools.document.Document
import pers.zhc.tools.epicycles.EpicyclesEdit
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity
import pers.zhc.tools.functiondrawing.FunctionDrawingBoard
import pers.zhc.tools.malloctest.MAllocTest
import pers.zhc.tools.pi.Pi
import pers.zhc.tools.test.*
import pers.zhc.tools.test.service.ServiceActivity
import pers.zhc.tools.test.viewtest.MainActivity
import pers.zhc.tools.theme.SetTheme
import pers.zhc.tools.toast.AToast
import pers.zhc.tools.youdaoapi.YouDaoTranslate
import pers.zhc.u.common.ReadIS
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * @author bczhc
 */
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tools_activity_main)
        init()
    }

    private fun shortcut(texts: IntArray, classes: Array<Class<*>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val sm = getSystemService(ShortcutManager::class.java)
            val choice = intArrayOf(0, 4, 7)
            if (sm != null && choice.size > sm.maxShortcutCountPerActivity) {
                return
            }
            val infoList: MutableList<ShortcutInfo> = ArrayList()
            for (i in choice.indices) {
                val builder = ShortcutInfo.Builder(this, "shortcut_id$i")
                val intent = Intent(this, classes[choice[i]])
                intent.action = Intent.ACTION_VIEW
                val shortcutInfo = builder.setShortLabel(getString(texts[choice[i]]))
                        .setLongLabel(getString(texts[choice[i]]))
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                        .setIntent(intent).build()
                infoList.add(shortcutInfo)
            }
            if (sm != null) {
                sm.dynamicShortcuts = infoList
            }
        }
    }

    private fun init() {
        val ll = findViewById<LinearLayout>(R.id.ll)
        val texts = intArrayOf(
                R.string.some_codecs,
                R.string.generate_pi,
                R.string.toast,
                R.string.put_in_clipboard,
                R.string.overlaid_drawing_board,
                R.string.fourier_series_calc,
                R.string.notes,
                R.string.fourier_series_in_complex,
                R.string.s,
                R.string.view_test,
                R.string.set_theme,
                R.string.math_expression_evaluation_test,
                R.string.sensor_test,
                R.string.input_event,
                R.string.surface_view_test,
                R.string.serviceTest,
                R.string.you_dao_translate_interface_invoke,
                R.string.crash_test,
                R.string.m_alloc_test
        )
        val classes = arrayOf<Class<*>>(
                CodecsActivity::class.java,
                Pi::class.java,
                AToast::class.java,
                Clip::class.java,
                FloatingDrawingBoardMainActivity::class.java,
                FunctionDrawingBoard::class.java,
                Document::class.java,
                EpicyclesEdit::class.java,
                S::class.java,
                MainActivity::class.java,
                SetTheme::class.java,
                MathExpressionEvaluationTest::class.java,
                SensorTest::class.java,
                InputEvent::class.java,
                SurfaceViewTest::class.java,
                ServiceActivity::class.java,
                YouDaoTranslate::class.java,
                CrashTest::class.java,
                MAllocTest::class.java
        )
        val mainTextLatch = CountDownLatch(1)
        Thread(Runnable {
            var jsonObject: JSONObject? = null
            try {
                val url = URL(Infos.ZHC_URL_STRING + "/tools_app/i.zhc")
                val inputStream = url.openStream()
                val sb = StringBuilder()
                ReadIS(inputStream, "utf-8").read { str: String? -> sb.append(str) }
                inputStream.close()
                jsonObject = JSONObject(sb.toString())
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            try {
                mainTextLatch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (jsonObject != null) {
                val finalJsonObject: JSONObject = jsonObject
                runOnUiThread {
                    try {
                        val mainActivityText = finalJsonObject.getString("MainActivityText")
                        val tv = TextView(this)
                        tv.text = getString(R.string.tv, mainActivityText)
                        ll.addView(tv)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }).start()
        Thread(Runnable {
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (i in texts.indices) {
                val btn = Button(this)
                btn.setText(texts[i])
                btn.textSize = 25f
                btn.isAllCaps = false
                btn.layoutParams = lp
                btn.setOnClickListener {
                    val intent = Intent()
                    intent.setClass(this, classes[i])
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_bottom, 0)
                }
                runOnUiThread { ll.addView(btn) }
            }
            mainTextLatch.countDown()
        }).start()
        shortcut(texts, classes)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }
}