package pers.zhc.tools.diary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.Cursor;
import pers.zhc.tools.utils.sqlite.Statement;
import pers.zhc.tools.views.ScrollEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

    /**
     * intent integer extra
     */
    public static String EXTRA_DATE_INT = "dateInt";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_taking_activity);

        updateStatement = this.diaryDatabase.compileStatement("UPDATE diary SET content=? WHERE date=?");

        et = ((ScrollEditText) findViewById(R.id.et)).getEditText();
        MaterialToolbar toolbar = findViewById(R.id.tool_bar);
        charactersCountTV = toolbar.findViewById(R.id.text_count_tv);
        SwitchMaterial ttsSwitch = toolbar.findViewById(R.id.tts_switch);
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

        et.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

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

        Handler debounceHandler = new Handler();
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

    private void ttsSpeak(String content, int queueMode) {
        Common.doAssertion(tts != null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int speak = tts.speak(content, queueMode, null, String.valueOf(System.currentTimeMillis()));
            if (speak != TextToSpeech.SUCCESS) {
                ToastUtils.showError(this, R.string.tts_speak_error, new Exception("Error code: " + speak));
            }
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

    static class MyDate {
        private int year, month, day;

        public MyDate(int dateInt) {
            set(dateInt);
        }

        public MyDate(@NotNull int[] date) {
            set(date);
        }

        public MyDate(@NotNull Date date) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            this.year = calendar.get(Calendar.YEAR);
            this.month = calendar.get(Calendar.MONTH) + 1;
            this.day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        public void set(int dateInt) {
            year = dateInt / 10000;
            month = (dateInt / 100) % 100;
            day = dateInt % 100;
        }

        public void set(@NotNull int[] date) {
            year = date[0];
            month = date[1];
            day = date[2];
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getDay() {
            return day;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyDate myDate = (MyDate) o;

            if (year != myDate.year) return false;
            if (month != myDate.month) return false;
            return day == myDate.day;
        }

        @Override
        public int hashCode() {
            int result = year;
            result = 31 * result + month;
            result = 31 * result + day;
            return result;
        }

        @NotNull
        @Override
        public String toString() {
            return year + "." + month + '.' + day;
        }

        private String add0(int a) {
            if (a < 10) return "0" + a;
            return String.valueOf(a);
        }

        public String getDateIntString() {
            return add0(year) + add0(month) + add0(day);
        }

        public int getDateInt() {
            return year * 10000 + month * 100 + day;
        }
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
}
