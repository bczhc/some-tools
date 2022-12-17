package pers.zhc.tools.diary

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.*
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import org.jetbrains.annotations.Contract
import pers.zhc.jni.sqlite.Statement
import pers.zhc.tools.R
import pers.zhc.tools.diary.DiaryAttachmentActivity
import pers.zhc.tools.diary.DiaryContentPreviewActivity.Companion.createDiaryRecordStatDialog
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.views.RegexInputView
import pers.zhc.tools.views.ScrollEditText
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author bczhc
 */
class DiaryTakingActivity : DiaryBaseActivity() {
    var live = true
    var speak = false
    private var tts: TextToSpeech? = null
    private var et: EditText? = null
    private var charactersCountTV: TextView? = null
    private var dateInt = 0
    private var updateStatement: Statement? = null
    private var saver: ScheduledSaver? = null
    private var ttsReplaceDict: MutableMap<String?, String>? = null
    private var findLayout: ViewGroup? = null
    private var setFoundCount: IntConsumer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_taking_activity)
        updateStatement = diaryDatabase.compileStatement("UPDATE diary SET content=? WHERE date=?")
        val scrollEditText = findViewById<ScrollEditText>(R.id.et)
        scrollEditText.setZoomFontSizeEnabled(true)
        et = scrollEditText.editText
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        charactersCountTV = toolbar.findViewById(R.id.text_count_tv)
        val ttsSwitch = toolbar.findViewById<MaterialSwitch>(R.id.tts_switch)
        val cancelButton = findViewById<ImageButton>(R.id.cancel_button)
        findLayout = findViewById(R.id.find_layout)
        val findInputView = findViewById<RegexInputView>(R.id.find_et)
        cancelButton.setOnClickListener { v: View? -> findLayout!!.setVisibility(View.GONE) }
        findInputView.regexChangeListener = { regex: Regex? ->
            highlightFind(regex!!)
            Unit
        }
        setFoundCount =
            object : IntConsumer {
                override fun call(count: Int) {
                    findInputView.shet.inputLayout.suffixText = Integer.toString(count)
                }
            }

        // set single-line and have ACTION_GO IME action
        findInputView.shet.editText.inputType = InputType.TYPE_CLASS_TEXT
        findInputView.shet.editText.imeOptions = EditorInfo.IME_ACTION_GO

        // On IME_ACTION_GO, invalidate text highlights, instead of refreshing on
        // EditText text changed - like Chrome, maybe for reducing lags
        findInputView.shet.editText.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val regex = findInputView.regex
                regex?.let { highlightFind(it) }
                return@setOnEditorActionListener true
            }
            false
        }
        ttsSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            speak = isChecked
            if (isChecked) {
                tts = TextToSpeech(this@DiaryTakingActivity, null)
            }
        }
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            onMenuItemClick(item)
            true
        }
        et!!.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN)
        ttsReplaceDict = HashMap()
        ttsReplaceDict!!["。"] = "句号"
        ttsReplaceDict!!["，"] = "逗号"
        ttsReplaceDict!!["\n"] = "换行"
        ttsReplaceDict!!["["] = "左方括号"
        ttsReplaceDict!!["]"] = "右方括号"
        ttsReplaceDict!!["“"] = "上引号"
        ttsReplaceDict!!["”"] = "下引号"
        ttsReplaceDict!!["‘"] = "上引号"
        ttsReplaceDict!!["’"] = "下引号"
        ttsReplaceDict!![" "] = "空格"
        ttsReplaceDict!!["、"] = "顿号"
        ttsReplaceDict!!["…"] = "省略号"
        ttsReplaceDict!!["……"] = "省略号"
        val debounceHandler = Handler(Looper.myLooper()!!)
        val watcher: TextWatcher = object : TextWatcher {
            private var last: String? = null
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                debounceHandler.removeCallbacksAndMessages(null)
                debounceHandler.postDelayed({ showCharactersCount() }, 2000)
                if (speak) {
                    last = s.toString()
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (ttsReplaceDict == null) {
                    ttsReplaceDict = HashMap()
                    ttsReplaceDict!!["。"] = "句点"
                    ttsReplaceDict!!["，"] = "逗号"
                    ttsReplaceDict!!["\n"] = "换行"
                    ttsReplaceDict!!["["] = "左方括号"
                    ttsReplaceDict!!["]"] = "右方括号"
                }
                if (speak) {
                    if (count < before) {
                        //delete
                        ttsSpeak(
                            getString(R.string.deleted_xxx, last!!.subSequence(start, start + before)),
                            TextToSpeech.QUEUE_FLUSH
                        )
                    } else {
                        //insert
                        var changed: String? = s.subSequence(start, start + count).toString()
                        if (ttsReplaceDict!!.containsKey(changed)) {
                            changed = ttsReplaceDict!!.get(changed)
                        }
                        ttsSpeak(changed)
                    }
                }
            }

            @Contract(pure = true)
            override fun afterTextChanged(s: Editable) {
            }
        }
        et!!.addTextChangedListener(watcher)
        val intent = intent
        if (intent.getIntExtra(EXTRA_DATE_INT, -1).also { dateInt = it } == -1) {
            throw RuntimeException("No dateInt provided.")
        }
        val hasRecord = diaryDatabase.hasRecord(
            """SELECT *
FROM diary
WHERE "date" IS ?""", arrayOf<Any>(dateInt)
        )
        val resultIntent = Intent()
        val newRec = !hasRecord
        resultIntent.putExtra("newRec", newRec)
        resultIntent.putExtra(EXTRA_DATE_INT, dateInt)
        setResult(0, resultIntent)
        prepareContent()
        if (newRec) {
            createNewRecord()
        }
        saver = ScheduledSaver()
        saver!!.start()
    }

    private fun highlightFind(regex: Regex) {
        // avoid finding with an empty pattern
        if (regex.pattern.isEmpty()) {
            // clear all colored span
            et!!.setText(et!!.text.toString())
            setFoundCount!!.call(0)
            return
        }
        val spannableString = SpannableString(et!!.text.toString())
        val iterator = regex.findAll(et!!.text.toString(), 0).iterator()
        var count = 0
        while (iterator.hasNext()) {
            val matchResult = iterator.next()
            val range = matchResult.range
            spannableString.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                range.first,
                range.last + 1 /* this argument is exclusive */,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ++count
        }
        setFoundCount!!.call(count)
        et!!.setText(spannableString)
    }

    private fun ttsSpeak(content: String?, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        Common.doAssertion(tts != null)
        val speak = tts!!.speak(content, queueMode, null, System.currentTimeMillis().toString())
        if (speak != TextToSpeech.SUCCESS) {
            ToastUtils.showError(this, R.string.tts_speak_error, Exception("Error code: $speak"))
        }
    }

    private fun createNewRecord() {
        val statement = diaryDatabase.compileStatement(
            """
    INSERT INTO diary("date", content)
    VALUES (?, ?)
    """.trimIndent()
        )
        statement.bind(1, dateInt)
        statement.bindText(2, "")
        statement.step()
        statement.release()
    }

    private fun showCharactersCount() {
        charactersCountTV!!.text = getString(R.string.characters_count_tv, et!!.length())
    }

    private fun prepareContent() {
        val statement = diaryDatabase.compileStatement(
            """SELECT content
FROM diary
WHERE "date" IS ?"""
        )
        statement.bind(1, dateInt)
        val cursor = statement.cursor
        if (cursor.step()) {
            val content = cursor.getText(0)
            et!!.setText(content)
        }
        statement.release()
        showCharactersCount()
    }

    private fun insertTime() {
        val date = Date()
        @SuppressLint("SimpleDateFormat") val time = SimpleDateFormat("[HH:mm]").format(date)
        et!!.text.insert(et!!.selectionStart, getString(R.string.str, time))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.diary_taking_actionbar, menu)
        return true
    }

    private fun onMenuItemClick(item: MenuItem) {
        val itemId = item.itemId
        if (itemId == R.id.record_time) {
            insertTime()
        } else if (itemId == R.id.attachment) {
            val intent = Intent(this, DiaryAttachmentActivity::class.java)
            intent.putExtra(DiaryAttachmentActivity.EXTRA_FROM_DIARY, true)
            intent.putExtra(DiaryAttachmentActivity.EXTRA_DATE_INT, dateInt)
            startActivity(intent)
        } else if (itemId == R.id.find) {

            // toggle search layout's visibility
            val visibility = findLayout!!.visibility
            if (visibility == View.GONE) findLayout!!.visibility =
                View.VISIBLE else if (visibility == View.VISIBLE) findLayout!!.visibility = View.GONE
        } else if (itemId == R.id.statistics) {
            createDiaryRecordStatDialog(this, diaryDatabase, dateInt).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        save()
        super.onSaveInstanceState(outState)
    }

    private fun save() {
        updateDiary(et!!.text.toString(), dateInt)
    }

    private fun updateDiary(content: String, dateString: Int) {
        try {
            updateStatement!!.reset()
            updateStatement!!.bindText(1, content)
            updateStatement!!.bind(2, dateString)
            updateStatement!!.step()
        } catch (e: Exception) {
            Common.showException(e, this)
        }
    }

    override fun finish() {
        live = false
        saver!!.stop()
        save()
        updateStatement!!.release()
        super.finish()
    }

    private inner class ScheduledSaver : Runnable {
        override fun run() {
            while (live) {
                if (Thread.interrupted()) break
                save()
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                    break
                }
                Log.d(TAG, "save diary...")
            }
        }

        private val t: Thread

        init {
            t = Thread(this)
        }

        fun start() {
            t.start()
        }

        fun stop() {
            t.interrupt()
        }
    }

    private interface IntConsumer {
        fun call(count: Int)
    }

    companion object {
        /**
         * intent integer extra
         */
        var EXTRA_DATE_INT = "dateInt"
    }
}