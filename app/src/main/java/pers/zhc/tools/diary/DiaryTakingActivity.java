package pers.zhc.tools.diary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import kotlin.Unit;
import kotlin.ranges.IntRange;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.views.RegexInputView;
import pers.zhc.tools.views.ScrollEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

/**
 * @author bczhc
 */
public class DiaryTakingActivity extends DiaryBaseActivity {

    boolean live = true;
    boolean speak = false;
    private TextToSpeech tts;
    private EditText et;
    private TextView charactersCountTV;
    private int dateInt;
    private Statement updateStatement;
    private ScheduledSaver saver;
    private Map<String, String> ttsReplaceDict = null;
    private ViewGroup findLayout;

    /**
     * intent integer extra
     */
    public static String EXTRA_DATE_INT = "dateInt";
    private IntConsumer setFoundCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_taking_activity);

        updateStatement = this.diaryDatabase.compileStatement("UPDATE diary SET content=? WHERE date=?");

        final ScrollEditText scrollEditText = findViewById(R.id.et);
        scrollEditText.setZoomFontSizeEnabled(true);

        et = scrollEditText.getEditText();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        charactersCountTV = toolbar.findViewById(R.id.text_count_tv);
        SwitchMaterial ttsSwitch = toolbar.findViewById(R.id.tts_switch);
        ImageButton cancelButton = findViewById(R.id.cancel_button);
        findLayout = findViewById(R.id.find_layout);
        RegexInputView findInputView = findViewById(R.id.find_et);

        cancelButton.setOnClickListener(v -> findLayout.setVisibility(View.GONE));

        findInputView.setRegexChangeListener(regex -> {
            highlightFind(regex);
            return Unit.INSTANCE;
        });

        setFoundCount = (int count) -> findInputView.shet.inputLayout.setSuffixText(Integer.toString(count));

        // set single-line and have ACTION_GO IME action
        findInputView.shet.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        findInputView.shet.getEditText().setImeOptions(EditorInfo.IME_ACTION_GO);

        // On IME_ACTION_GO, invalidate text highlights, instead of refreshing on
        // EditText text changed - like Chrome, maybe for reducing lags
        findInputView.shet.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                final Regex regex = findInputView.getRegex();
                if (regex != null) {
                    highlightFind(regex);
                }
                return true;
            }
            return false;
        });

        ttsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            speak = isChecked;
            if (isChecked) {
                tts = new TextToSpeech(DiaryTakingActivity.this, null);
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            onMenuItemClick(item);
            return true;
        });

        et.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);

        ttsReplaceDict = new HashMap<>();
        ttsReplaceDict.put("。", "句号");
        ttsReplaceDict.put("，", "逗号");
        ttsReplaceDict.put("\n", "换行");
        ttsReplaceDict.put("[", "左方括号");
        ttsReplaceDict.put("]", "右方括号");
        ttsReplaceDict.put("“", "上引号");
        ttsReplaceDict.put("”", "下引号");
        ttsReplaceDict.put("‘", "上引号");
        ttsReplaceDict.put("’", "下引号");
        ttsReplaceDict.put(" ", "空格");
        ttsReplaceDict.put("、", "顿号");
        ttsReplaceDict.put("…", "省略号");
        ttsReplaceDict.put("……", "省略号");

        Handler debounceHandler = new Handler(Looper.myLooper());
        final TextWatcher watcher = new TextWatcher() {
            private String last;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                debounceHandler.removeCallbacksAndMessages(null);
                debounceHandler.postDelayed(() -> showCharactersCount(), 2000);
                if (speak) {
                    last = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ttsReplaceDict == null) {
                    ttsReplaceDict = new HashMap<>();
                    ttsReplaceDict.put("。", "句点");
                    ttsReplaceDict.put("，", "逗号");
                    ttsReplaceDict.put("\n", "换行");
                    ttsReplaceDict.put("[", "左方括号");
                    ttsReplaceDict.put("]", "右方括号");
                }

                if (speak) {
                    if (count < before) {
                        //delete
                        ttsSpeak(getString(R.string.deleted_xxx, last.subSequence(start, start + before)), QUEUE_FLUSH);
                    } else {
                        //insert
                        String changed = s.subSequence(start, start + count).toString();
                        if (ttsReplaceDict.containsKey(changed)) {
                            changed = ttsReplaceDict.get(changed);
                        }
                        ttsSpeak(changed);
                    }
                }
            }

            @Contract(pure = true)
            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        et.addTextChangedListener(watcher);

        final Intent intent = getIntent();
        if ((dateInt = intent.getIntExtra(EXTRA_DATE_INT, -1)) == -1) {
            throw new RuntimeException("No dateInt provided.");
        }

        final boolean hasRecord = this.diaryDatabase.hasRecord("SELECT *\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?", new Object[]{dateInt});

        Intent resultIntent = new Intent();
        final boolean newRec = !hasRecord;
        resultIntent.putExtra("newRec", newRec);
        resultIntent.putExtra(EXTRA_DATE_INT, dateInt);
        setResult(0, resultIntent);

        prepareContent();

        if (newRec) {
            createNewRecord();
        }

        saver = new ScheduledSaver();
        saver.start();
    }

    private void highlightFind(@NotNull Regex regex) {
        // avoid finding with an empty pattern
        if (regex.getPattern().isEmpty()) {
            // clear all colored span
            et.setText(et.getText().toString());
            setFoundCount.call(0);
            return;
        }
        final SpannableString spannableString = new SpannableString(et.getText().toString());

        final Iterator<MatchResult> iterator = regex.findAll(et.getText().toString(), 0).iterator();
        int count = 0;
        while (iterator.hasNext()) {
            MatchResult matchResult = iterator.next();
            final IntRange range = matchResult.getRange();

            spannableString.setSpan(
                    new BackgroundColorSpan(Color.YELLOW),
                    range.getFirst(),
                    range.getLast() + 1 /* this argument is exclusive */,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            ++count;
        }

        setFoundCount.call(count);
        et.setText(spannableString);
    }

    private void ttsSpeak(String content, int queueMode) {
        Common.doAssertion(tts != null);
        final int speak = tts.speak(content, queueMode, null, String.valueOf(System.currentTimeMillis()));
        if (speak != TextToSpeech.SUCCESS) {
            ToastUtils.showError(this, R.string.tts_speak_error, new Exception("Error code: " + speak));
        }
    }

    private void ttsSpeak(String content) {
        ttsSpeak(content, QUEUE_ADD);
    }

    private void createNewRecord() {
        final Statement statement = diaryDatabase.compileStatement("INSERT INTO diary(\"date\", content)\n" +
                "VALUES (?, ?)");
        statement.bind(1, dateInt);
        statement.bindText(2, "");
        statement.step();
        statement.release();
    }

    private void showCharactersCount() {
        charactersCountTV.setText(getString(R.string.characters_count_tv, et.length()));
    }

    private void prepareContent() {
        final Statement statement = diaryDatabase.compileStatement("SELECT content\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?");
        statement.bind(1, dateInt);
        final Cursor cursor = statement.getCursor();
        if (cursor.step()) {
            final String content = cursor.getText(0);
            et.setText(content);
        }
        statement.release();
        showCharactersCount();
    }

    private void insertTime() {
        final Date date = new Date();
        @SuppressLint("SimpleDateFormat") final String time = new SimpleDateFormat("[HH:mm]").format(date);
        et.getText().insert(et.getSelectionStart(), getString(R.string.str, time));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.diary_taking_actionbar, menu);
        return true;
    }

    private void onMenuItemClick(@NotNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.record_time) {

            insertTime();

        } else if (itemId == R.id.attachment) {

            Intent intent = new Intent(this, DiaryAttachmentActivity.class);
            intent.putExtra(DiaryAttachmentActivity.EXTRA_FROM_DIARY, true);
            intent.putExtra(DiaryAttachmentActivity.EXTRA_DATE_INT, dateInt);
            startActivity(intent);

        } else if (itemId == R.id.find) {

            // toggle search layout's visibility
            final int visibility = findLayout.getVisibility();
            if (visibility == View.GONE) findLayout.setVisibility(View.VISIBLE);
            else if (visibility == View.VISIBLE) findLayout.setVisibility(View.GONE);

        } else if (itemId == R.id.statistics) {

            DiaryContentPreviewActivity.Companion.createDiaryRecordStatDialog(this, diaryDatabase, dateInt).show();

        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        save();
        super.onSaveInstanceState(outState);
    }

    private void save() {
        updateDiary(et.getText().toString(), dateInt);
    }

    private void updateDiary(String content, int dateString) {
        try {
            updateStatement.reset();
            updateStatement.bindText(1, content);
            updateStatement.bind(2, dateString);
            updateStatement.step();
        } catch (Exception e) {
            Common.showException(e, this);
        }
    }

    @Override
    public void finish() {
        live = false;
        saver.stop();
        save();
        updateStatement.release();
        super.finish();
    }

    private class ScheduledSaver implements Runnable {
        @Override
        public void run() {
            while (live) {
                if (Thread.interrupted()) break;
                save();
                try {
                    //noinspection BusyWait
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                    break;
                }
                Log.d(TAG, "save diary...");
            }
        }

        private final Thread t;

        public ScheduledSaver() {
            t = new Thread(this);
        }

        private void start() {
            t.start();
        }

        private void stop() {
            t.interrupt();
        }
    }

    private interface IntConsumer {
        void call(int count);
    }
}
