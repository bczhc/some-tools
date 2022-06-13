package pers.zhc.tools.charucd

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import kotlinx.android.synthetic.main.char_ucd_lookup_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import java.io.File
import java.net.URL

/**
 * @author bczhc
 */
class CharLookupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.char_ucd_lookup_activity)

        val charLookupView = lookup_view!!
        val button = btn!!
        val getUcdDbButton = get_btn!!

        button.setOnClickListener {
            val codepoint = charLookupView.getCodepoint().let {
                if (it == null) {
                    ToastUtils.show(this, R.string.please_enter_correct_value_toast)
                    return@setOnClickListener
                }
                it
            }

            val intent = Intent(this, CharUcdActivity::class.java)
            intent.putExtra(CharUcdActivity.EXTRA_CODEPOINT, codepoint)
            startActivity(intent)
        }

        getUcdDbButton.setOnClickListener {
            DialogUtils.createConfirmationAlertDialog(
                this,
                { _, _ ->
                    fetchAndProcessDatabase()
                },
                titleRes = R.string.char_ucd_download_ucd_database_prompt_dialog,
                message = getString(R.string.char_ucd_process_ucd_data_dialog_msg)
            ).apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }.show()
        }
    }

    private fun fetchAndProcessDatabase() {
        val paths = object {
            val download = File(filesDir, "ucd-xml.zip")
            val database = UCD_DATABASE_PATH
        }

        val progressView = ParseProgressView(this).apply {
            setActionText(ParseAction.DOWNLOADING.getActionMsg(this@CharLookupActivity))
        }
        val dialog = Dialog(this).apply {
            setContentView(progressView)
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        dialog.show()

        val tryDo = AsyncTryDo()

        val handler = Handler(Looper.getMainLooper())

        Thread {
            val progressViewOnUiThread = object {
                val resetProgress = {
                    handler.post { progressView.setProgress(0F, false) }
                    Unit
                }
                val setActionText = { s: String -> handler.post { progressView.setActionText(s) } }
            }

            val os = paths.download.outputStream()
            // the `download` method is synchronous, so we can write tasks below it directly; pass `null` to `doneAction`
            Download.download(URL(UNICODE_UCD_XML_URL), os, {
                tryDo.tryDo { _, notifier ->
                    handler.post {
                        progressView.setProgressAndTitle(it)
                        notifier.finish()
                    }
                }
            }, null)
            os.close()

            progressViewOnUiThread.setActionText(ParseAction.COUNTING.getActionMsg(this))
            progressViewOnUiThread.resetProgress()
            val count = JNI.CharUcd.count(paths.download.path) {
                tryDo.tryDo { _, notifier ->
                    handler.post {
                        progressView.setProgressTitle(getString(R.string.char_ucd_parse_counting_entries, it))
                        notifier.finish()
                    }
                }
            }

            progressViewOnUiThread.setActionText(ParseAction.PARSING_XML.getActionMsg(this))
            progressViewOnUiThread.resetProgress()
            // delete, and recreate it (SQLite3 "open" function automatically does)
            paths.database.requireDelete()
            JNI.CharUcd.parseXml(paths.download.path, paths.database.path) {
                tryDo.tryDo { _, notifier ->
                    handler.post {
                        progressView.setProgressAndTitle(it.toFloat() / count.toFloat())
                        notifier.finish()
                    }
                }
            }
            paths.download.requireDelete()

            handler.post {
                progressView.setProgress(1F)
                dialog.dismiss()
                ToastUtils.show(this, R.string.char_ucd_parse_done_msg)
            }

        }.start()
    }

    private enum class ParseAction(val msgStrRes: Int) {
        DOWNLOADING(R.string.char_ucd_parse_downloading_action_msg),
        COUNTING(R.string.char_ucd_parse_counting_action_msg),
        PARSING_XML(R.string.char_ucd_parse_processing_xml_action_msg);

        fun getActionMsg(context: Context): String {
            return context.getString(this.msgStrRes)
        }
    }

    companion object {
        private const val UNICODE_UCD_XML_URL = "https://www.unicode.org/Public/UCD/latest/ucdxml/ucd.all.flat.zip"

        lateinit var UCD_DATABASE_PATH: File

        fun init(context: Context) {
            UCD_DATABASE_PATH = File(context.filesDir, "ucd.db")
        }
    }
}