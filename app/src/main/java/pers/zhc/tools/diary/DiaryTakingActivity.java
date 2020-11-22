package pers.zhc.tools.diary;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ScrollEditText;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.MySQLite3;
import pers.zhc.tools.utils.sqlite.SQLite;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author bczhc
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DiaryTakingActivity extends BaseActivity {

    private EditText et;
    private TextView charactersCountTV;
    private MyDate mDate;
    private MySQLite3 diaryDatabase = DiaryMainActivity.diaryDatabase;
    private Timer savingTimer;
    boolean live = true;
    boolean speak = false;
    TextToSpeech tts;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        savingTimer = new Timer();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_taking_activity);
        et = ((ScrollEditText) findViewById(R.id.et)).getEditText();
        et.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        Handler debounceHandler = new Handler();
        et.addTextChangedListener(new TextWatcher() {
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
                if (speak) {
                    if (count < before) {
                        //delete
                        tts.speak(getString(R.string.deleted_xxx, last.subSequence(start, start + before)), TextToSpeech.QUEUE_FLUSH, null, "");
                    } else {
                        //insert
                        CharSequence changed = s.subSequence(start, start + count);
                        switch (changed.toString()) {
                            case "。":
                                changed = "句点";
                                break;
                            case "，":
                                changed = "逗号";
                                break;
                            default:
                                break;
                        }
                        tts.speak(changed, TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis()));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.diary);
            charactersCountTV = new TextView(this);
            charactersCountTV.setTextColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                charactersCountTV.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
            actionBar.setCustomView(charactersCountTV);
            actionBar.show();
        }
        final Intent intent = getIntent();
        int[] date = intent.getIntArrayExtra("date");
        if (date == null) {
            final Calendar calendar = Calendar.getInstance();
            date = new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)};
        }
        mDate = new MyDate(date);
        initDB();
        prepareContent();
        savingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (live) {
                    save();
                    Log.d(TAG, "saved diary");
                }
            }
        }, 10000);
    }

    private void showCharactersCount() {
        charactersCountTV.setText(getString(R.string.characters_count_tv, et.length()));
    }

    private void prepareContent() {
        if (diaryDatabase.isClosed()) {
            ToastUtils.show(this, R.string.closed);
            return;
        }
        final String[] content = {null};
        diaryDatabase.exec("SELECT content FROM diary WHERE date='" + mDate.getDateString() + "'", contents -> {
            if (content[0] == null)
                content[0] = contents[0];
            return 0;
        });
        if (content[0] != null) {
            et.setText(content[0]);
            showCharactersCount();
        }
    }

    private void recordTime() {
        final Date date = new Date();
        @SuppressLint("SimpleDateFormat") final String time = new SimpleDateFormat("[HH:mm]").format(date);
        et.getText().insert(et.getSelectionStart(), getString(R.string.str, time));
    }

    private void initDB() {
        if (diaryDatabase.isClosed()) {
            ToastUtils.show(this, R.string.closed);
            return;
        }
        boolean newRec = !SQLite.checkRecordExistence(diaryDatabase, "diary", "date", mDate.getDateString());
        if (newRec) {
            diaryDatabase.exec("INSERT INTO diary VALUES('" + mDate.getDateString() + "','')");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.speak_switch);
        Switch speakSwitch = new Switch(this);
        speakSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            speak = isChecked;
            if (isChecked) {
                tts = new TextToSpeech(DiaryTakingActivity.this, null);
            }
        });
        speakSwitch.setText(R.string.voice);
        item.setActionView(speakSwitch);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.diary_taking_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.record_time) {
            recordTime();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        save();
        live = false;
        savingTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        save();
        super.onSaveInstanceState(outState);
    }

    private void save() {
        if (diaryDatabase.isClosed()) {
            runOnUiThread(() -> {
                ToastUtils.show(this, R.string.closed);
            });
            return;
        }
        diaryDatabase.exec("UPDATE diary SET content='" + et.getText().toString().replace("'", "''") + "' WHERE date='" + mDate.getDateString() + "'");
    }

    static class MyDate {
        int year, month, day;

        public MyDate(int date) {
            year = date / 10000;
            month = (date / 100) % 100;
            day = date % 100;
        }

        public MyDate(int[] date) {
            year = date[0];
            month = date[1];
            day = date[2];
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

        public String getDateString() {
            return add0(year) + add0(month) + add0(day);
        }
    }
}
