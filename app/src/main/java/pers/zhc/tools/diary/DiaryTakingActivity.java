package pers.zhc.tools.diary;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.ScrollEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author bczhc
 */
public class DiaryTakingActivity extends BaseActivity {

    private ScheduledExecutorService ses;
    private EditText et;
    private TextView infoTV;
    private MyDate mDate;
    private SQLiteDatabase diaryDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_taking_activity);
        diaryDatabase = DiaryMainActivity.getDiaryDatabase(this);
        et = ((ScrollEditText) findViewById(R.id.et)).getEditText();
        startTimer();
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.diary);
            infoTV = new TextView(this);
            infoTV.setTextColor(Color.WHITE);
            infoTV.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            actionBar.setCustomView(infoTV);
            actionBar.show();
        }
        final Intent intent = getIntent();
        int[] date = intent.getIntArrayExtra("date");
        if (date == null) {
            final Calendar calendar = Calendar.getInstance();
            date = new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)};
        }
        mDate = new MyDate(date);
        initDB();
        prepareContent();
    }

    private void prepareContent() {
        final Cursor cursor = diaryDatabase.rawQuery("SELECT content FROM diary WHERE date=?", new String[]{mDate.toString()});
        String content = null;
        if (cursor.moveToFirst()) {
            content = cursor.getString(cursor.getColumnIndex("content"));
        }
        cursor.close();
        if (content != null) {
            et.setText(content);
        }
    }

    private void recordTime() {
        final Date date = new Date();
        final String time = new SimpleDateFormat("[HH:mm]").format(date);
        et.setText(String.valueOf(et.getText()) + '\n' + time);
    }

    private void initDB() {
        final Cursor cursor = diaryDatabase.rawQuery("SELECT date FROM diary WHERE date=?", new String[]{this.mDate.toString()});
        final boolean newRec = cursor.getCount() == 0;
        cursor.close();
        if (newRec) {
            ContentValues cv = new ContentValues();
            cv.put("date", mDate.toString());
            cv.put("content", "");
            try {
                diaryDatabase.insertOrThrow("diary", null, cv);
            } catch (SQLException e) {
                Common.showException(e, this);
            }
        }
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

    private void startTimer() {
        ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(() -> runOnUiThread(this::save), 0, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void onDestroy() {
        ses.shutdown();
        save();
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
        ContentValues cv = new ContentValues();
        cv.put("date", mDate.toString());
        cv.put("content", this.et.getText().toString());
        diaryDatabase.update("diary", cv, "date=?", new String[]{mDate.toString()});
        final Date date = new Date();
        final String time = new SimpleDateFormat("-HH:mm:ss").format(date);
        infoTV.setText(getString(R.string.saved) + time);
    }

    static class MyDate {
        int year, month, day;

        MyDate(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        MyDate() {
        }

        MyDate(int[] date) {
            year = date[0];
            month = date[1];
            day = date[2];
        }

        MyDate(String dateString) {
            final String[] split = dateString.split("\\.");
            year = Integer.parseInt(split[0]);
            month = Integer.parseInt(split[1]);
            day = Integer.parseInt(split[2]);
        }

        @Override
        public String toString() {
            return year + "." + month + "." + day;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MyDate myDate = (MyDate) o;
            if (year != myDate.year) {
                return false;
            }
            if (month != myDate.month) {
                return false;
            }
            return day == myDate.day;
        }

        @Override
        public int hashCode() {
            int result = year;
            result = 31 * result + month;
            result = 31 * result + day;
            return result;
        }
    }
}
