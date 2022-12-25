package pers.zhc.tools.diary

import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.widget.addTextChangedListener
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryTakingActivityBinding
import pers.zhc.tools.diary.DiaryContentPreviewActivity.Companion.createDiaryRecordStatDialog
import pers.zhc.tools.utils.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author bczhc
 */
class DiaryTakingActivity : DiaryBaseActivity() {
    private var autoSaverRunning = true
    private var ttsEnabled = false
    private var tts: TextToSpeech? = null
    private lateinit var editText: EditText
    private lateinit var charactersCountTV: TextView
    private var dateInt = 0
    private var saver: ScheduledSaver? = null
    private val ttsReplaceDict by lazy { createTtsReplaceDict() }
    private lateinit var findLayout: ViewGroup
    private lateinit var setFoundCount: (count: Int) -> Unit
    private lateinit var bindings: DiaryTakingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = DiaryTakingActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        editText = bindings.et.let {
            it.setZoomFontSizeEnabled(true)
            it.editText
        }
        val toolbar = bindings.toolbar
        charactersCountTV = bindings.textCountTv
        val ttsSwitch = bindings.ttsSwitch
        val cancelButton = bindings.cancelButton
        findLayout = bindings.findLayout
        val findInputView = bindings.findEt

        cancelButton.setOnClickListener {
            findLayout.visibility = View.GONE
        }
        findInputView.regexChangeListener = { regex ->
            highlightFind(regex!!)
        }
        setFoundCount = { count ->
            findInputView.shet.inputLayout.suffixText = count.toString()
        }

        // set single-line and have ACTION_GO IME action
        findInputView.shet.editText.apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_GO
        }

        // On IME_ACTION_GO, invalidate text highlights, instead of refreshing on
        // EditText text changed - like Chrome, maybe for reducing lags
        findInputView.shet.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                findInputView.regex?.let { highlightFind(it) }
                return@setOnEditorActionListener true
            }
            false
        }
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            ttsEnabled = isChecked
            if (isChecked) {
                tts = TextToSpeech(this@DiaryTakingActivity, null)
            }
        }
        toolbar.setOnMenuItemClickListener { item ->
            onMenuItemClick(item)
            true
        }
        editText.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

        val debounceHandler = Handler(Looper.myLooper()!!)
        var last: String? = null
        editText.addTextChangedListener(
            beforeTextChanged = { s, _, _, _ ->
                debounceHandler.removeCallbacksAndMessages(null)
                debounceHandler.postDelayed({ showCharactersCount() }, 2000)
                if (ttsEnabled) {
                    last = s.toString()
                }
            },
            onTextChanged = { s, start, before, count ->
                if (ttsEnabled) {
                    if (count < before) {
                        //delete
                        ttsSpeak(
                            getString(R.string.deleted_xxx, last!!.subSequence(start, start + before)),
                            TextToSpeech.QUEUE_FLUSH
                        )
                    } else {
                        //insert
                        val ttsText = s!!.subSequence(start, start + count).toString().let {
                            ttsReplaceDict[it] ?: it
                        }
                        ttsSpeak(ttsText)
                    }
                }
            }
        )

        val intent = intent
        if (!intent.hasExtra(EXTRA_DATE_INT)) {
            throw RuntimeException("No dateInt provided.")
        }
        dateInt = intent.getIntExtra(EXTRA_DATE_INT, -1)

        val hasRecord = diaryDatabase.hasDiary(dateInt)
        val newRec = !hasRecord
        val resultIntent = Intent().apply {
            putExtra(EXTRA_NEW_REC, newRec)
            putExtra(EXTRA_DATE_INT, dateInt)
        }
        setResult(0, resultIntent)
        prepareContent()
        if (newRec) {
            createNewRecord()
        }
        saver = ScheduledSaver().also { it.start() }
    }

    private fun highlightFind(regex: Regex) {
        // avoid finding with an empty pattern
        if (regex.pattern.isEmpty()) {
            // clear all colored span
            editText.setText(editText.text.toString())
            setFoundCount(0)
            return
        }
        val spannableString = SpannableString(editText.text.toString())
        val results = regex.findAll(editText.text.toString(), 0).toList()
        for (matchResult in results) {
            val range = matchResult.range
            spannableString.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                range.first,
                range.last + 1, /* this argument is exclusive */
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        setFoundCount(results.size)
        editText.setText(spannableString)
    }

    private fun ttsSpeak(content: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        val speak = tts!!.speak(content, queueMode, null, System.currentTimeMillis().toString())
        if (speak != TextToSpeech.SUCCESS) {
            ToastUtils.showError(this, R.string.tts_speak_error, Exception("Error code: $speak"))
        }
    }

    private fun createNewRecord() {
        val statement = diaryDatabase.database.compileStatement(
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
        charactersCountTV.text = getString(R.string.characters_count_tv, editText.length())
    }

    private fun prepareContent() {
        val statement = diaryDatabase.database.compileStatement(
            """SELECT content
FROM diary
WHERE "date" IS ?"""
        )
        statement.bind(1, dateInt)
        val cursor = statement.cursor
        if (cursor.step()) {
            val content = cursor.getText(0)
            editText.setText(content)
        }
        statement.release()
        showCharactersCount()
    }

    private fun insertTime() {
        val date = Date()
        @SuppressLint("SimpleDateFormat") val time = SimpleDateFormat("[HH:mm]").format(date)
        editText.text.insert(editText.selectionStart, getString(R.string.str, time))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.diary_taking_actionbar, menu)
        return true
    }

    private fun onMenuItemClick(item: MenuItem) {
        when (item.itemId) {
            R.id.record_time -> {
                insertTime()
            }

            R.id.attachment -> {
                val intent = Intent(this, DiaryAttachmentActivity::class.java).apply {
                    putExtra(DiaryAttachmentActivity.EXTRA_FROM_DIARY, true)
                    putExtra(DiaryAttachmentActivity.EXTRA_DATE_INT, dateInt)
                }
                startActivity(intent)
            }

            R.id.find -> {
                // toggle search layout's visibility
                val visibility = findLayout.visibility
                findLayout.visibility = when (visibility) {
                    View.GONE -> {
                        View.VISIBLE
                    }

                    View.VISIBLE -> {
                        View.GONE
                    }

                    else -> {
                        unreachable()
                    }
                }
            }

            R.id.statistics -> {
                createDiaryRecordStatDialog(this, diaryDatabase, dateInt).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        save()
        super.onSaveInstanceState(outState)
    }

    private fun save() {
        updateDiary(dateInt, editText.text.toString())
    }

    private fun updateDiary(dateInt: Int, content: String) {
        try {
            diaryDatabase.updateDiaryContent(dateInt, content)
        } catch (e: Exception) {
            Common.showException(e, this)
        }
    }

    override fun finish() {
        autoSaverRunning = false
        saver!!.stop()
        save()
        super.finish()
    }

    private inner class ScheduledSaver : Runnable {
        override fun run() {
            while (autoSaverRunning) {
                if (Thread.interrupted()) break
                save()
                try {
                    Thread.sleep(10000)
                } catch (_: InterruptedException) {
                    break
                }
                Log.d(TAG, "save diary...")
            }
        }

        private val t: Thread = Thread(this)

        fun start() {
            t.start()
        }

        fun stop() {
            t.interrupt()
        }
    }

    private fun createTtsReplaceDict(): HashMap<String, String> {
        return hashMapOf(
            Pair("。", "句号"),
            Pair("，", "逗号"),
            Pair("\n", "换行"),
            Pair("[", "左方括号"),
            Pair("]", "右方括号"),
            Pair("“", "上引号"),
            Pair("”", "下引号"),
            Pair("‘", "上引号"),
            Pair("’", "下引号"),
            Pair(" ", "空格"),
            Pair("、", "顿号"),
            Pair("…", "省略号"),
            Pair("……", "省略号"),
        )
    }

    class ActivityContract: ActivityResultContract<IntDate, ActivityContract.Result>() {
        class Result(
            val dateInt: Int,
            val isNewRecord: Boolean
        )

        override fun createIntent(context: Context, input: IntDate): Intent {
            return Intent(context, DiaryTakingActivity::class.java).apply {
                putExtra(EXTRA_DATE_INT, input.dateInt)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Result {
            intent!!
            val dateInt = intent.getIntExtraOrNull(EXTRA_DATE_INT)!!
            val newRecord = intent.getBooleanExtraOrNull(EXTRA_NEW_REC)!!
            return Result(dateInt, newRecord)
        }
    }

    companion object {
        /**
         * integer intent extra
         */
        const val EXTRA_DATE_INT = "dateInt"

        /**
         * boolean intent extra
         * true indicates the diary with this [dateInt] doesn't exist and will be created
         */
        const val EXTRA_NEW_REC = "newRec"
    }
}