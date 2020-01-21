package pers.zhc.tools.codecs;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pers.zhc.tools.utils.Common.showException;
import static pers.zhc.tools.utils.DisplayUtil.px2sp;

public class Settings extends BaseActivity {
    private Intent r_intent = new Intent();
    private LinearLayout ll;
    private boolean haveChanged = false;
    private CodecsActivity o = new CodecsActivity();
    private String[] jsonText = new String[]{
            "sourceExtension",
            "destExtension",
            "deleteOldFile"

    };
    private List<List<View>> lists;
    private JSONObject json;
    private CountDownLatch latch;
    private CountDownLatch latch1;
    private List<List<String>> saved;
    private String savedJSONText;
    private File file;
    private String[] dataList = null;
    private String[] jsonData = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CountDownLatch latch2 = new CountDownLatch(1);
        final Intent[] intent = new Intent[1];
        new Thread(() -> {
            this.dataList = this.getResources().getStringArray(R.array.settings_d_t);
            intent[0] = this.getIntent();
            this.file = (File) intent[0].getSerializableExtra("file");
            this.jsonData = this.getResources().getStringArray(R.array.json);
            latch2.countDown();
        }).start();
        setContentView(R.layout.settings_activity);
        ll = findViewById(R.id.ll);
        ExecutorService es = Executors.newCachedThreadPool();
        this.latch = new CountDownLatch(1);
        this.latch1 = new CountDownLatch(1);
        try {
            latch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        es.execute(() -> {
            try {
                if (!file.exists()) System.out.println("file.createNewFile() = " + file.createNewFile());
            } catch (IOException e) {
                showException(e, Settings.this);
            }
            this.lists = new ArrayList<>();
            this.json = new JSONObject();
            this.savedJSONText = intent[0].getStringExtra("jsonText");
            this.latch1.countDown();
            int length = dataList.length;
            LinearLayout[] linearLayouts = new LinearLayout[length];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int[] color = new int[]{
                    ContextCompat.getColor(this, R.color.s1),
                    ContextCompat.getColor(this, R.color.s2),
            };
            int[] textViewsText = new int[]{
                    R.string.filter,
                    R.string.iFileExtension,
                    R.string.oFileExtension
            };
            for (int i = 0; i < length; i++) {
                int finalI = i;
                linearLayouts[i] = new LinearLayout(this);
                linearLayouts[i].setOrientation(LinearLayout.VERTICAL);
                linearLayouts[i].setLayoutParams(lp);
                linearLayouts[i].setBackgroundColor(color[i & 1]);
                TextView option_tv = new TextView(this);
                option_tv.setGravity(Gravity.CENTER);
                option_tv.setText(dataList[i]);
                option_tv.setTextSize(25F);
//                EditText path_et = new EditText(this);
                EditText[] editTexts = new EditText[2];
                TextView[] textViews = new TextView[3];
                for (int j = 0; j < textViews.length; j++) {
                    textViews[j] = new TextView(this);
                    textViews[j].setText(textViewsText[j]);
                }
                List<View> viewList = new ArrayList<>();
                try {
                    this.latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < editTexts.length; j++) {
                    editTexts[j] = new EditText(this);
                    int finalJ = j;
                    runOnUiThread(() -> {
                        String s = null;
                        try {
                            s = this.saved.get(finalI).get(finalJ);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                        editTexts[finalJ].setText(s == null ? "" : s);
                    });
                    editTexts[j].setOnFocusChangeListener((v, hasFocus) -> this.haveChanged = true);
                    viewList.add(editTexts[j]);
                }

                float textSize = px2sp(Settings.this, textViews[1].getTextSize());
                textViews[0].setTextSize(textSize + 5);
                CheckBox[] checkBoxes = new CheckBox[length];
                CountDownLatch latch = new CountDownLatch(1);
                runOnUiThread(() -> {
                    linearLayouts[finalI].addView(option_tv);
                    linearLayouts[finalI].addView(textViews[0]);
                    for (int j = 0; j < 2; j++) {
                        linearLayouts[finalI].addView(textViews[j + 1]);
                        linearLayouts[finalI].addView(editTexts[j]);
                    }
                    checkBoxes[finalI] = new CheckBox(this);
                    checkBoxes[finalI].setText(R.string.delete_old_file);
                    try {
                        checkBoxes[finalI].setChecked(Boolean.parseBoolean(this.saved.get(finalI).get(2)));
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                    linearLayouts[finalI].addView(checkBoxes[finalI]);
                    ll.addView(linearLayouts[finalI]);
                    latch.countDown();
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> this.haveChanged = true);
                viewList.add(checkBoxes[i]);
                this.lists.add(viewList);
            }
            RelativeLayout rl = new RelativeLayout(this);
            rl.setGravity(Gravity.CENTER);
            RelativeLayout.LayoutParams rl_lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.setLayoutParams(rl_lp);
            Button okBtn = new Button(this);
            okBtn.setText(R.string.save);
            ViewGroup.LayoutParams btnLP = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            okBtn.setLayoutParams(btnLP);
            okBtn.setOnClickListener(v -> {
                saveAll();
                String s = null;
                try {
                    s = this.json.toString();
                    saveJSONText(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.r_intent.putExtra("result", s);
                returnIntent(this.r_intent);
            });
            runOnUiThread(() -> {
                rl.addView(okBtn);
                ll.addView(rl);
            });
        });

        es.execute(() -> {
            try {
                latch1.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread.currentThread() = " + Thread.currentThread());
            try {
//                this.o.spinnerData.addAll(Arrays.asList(this.dT));
                o.jsonData = this.jsonData;
                this.saved = this.o.solveJSON(this.savedJSONText);
            } catch (JSONException e) {
                e.printStackTrace();
                this.runOnUiThread(() -> ToastUtils.show(this, this.getString(R.string.json_solve_error) + e.toString()));
                try {
                    OutputStream os = new FileOutputStream(file, false);
                    os.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            this.latch.countDown();
        });
        es.shutdown();
    }

    private void returnIntent(Intent intent) {
        this.setResult(4, intent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if (this.haveChanged) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.whether_to_save)
                    .setNegativeButton(R.string.no, (dialog, which) -> this.finish())
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        saveAll();
                        String r = this.json.toString();
                        this.r_intent.putExtra("result", r);
                        try {
                            saveJSONText(r);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.returnIntent(r_intent);
                    })
                    .show();
        } else super.onBackPressed();
    }

//    /**
//     * Generate codecs_activity_a value suitable for use in .
//     * This value will not collide with ID values generated at build time by aapt for R.id.
//     *
//     * @return codecs_activity_a generated ID value
//     */
    /*private int generateViewId() {
        final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }*/

    private void saveAll() {
        try {
            for (int i = 0; i < this.lists.size(); i++) {
                JSONObject o = new JSONObject();
                for (int j = 0; j < /*this.lists.get(i).size() - 1*/ 2; j++) {
                    o.put(this.jsonText[j], ((EditText) this.lists.get(i).get(j)).getText().toString());
                }
                o.put(this.jsonText[2], ((CheckBox) this.lists.get(i).get(2)).isChecked());
                this.json.put(jsonData[i], o);
            }
        } catch (JSONException e) {
            showException(e, this);
        }
    }

    private void saveJSONText(String content) throws IOException {
        OutputStream os = new FileOutputStream(this.file);
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.flush();
        bw.close();
        osw.close();
        os.close();
    }
}

/*
class IIO {
    private List<IIOObject> list;

    IIO() {
        list = new ArrayList<>();
    }

    @SuppressWarnings("UnusedReturnValue")
    IIO put(int key1, int key2, Object o) {
        list.add(new IIOObject(key1, key2, o));
        return this;
    }

    */
/*Object get(int key1, int key2) {
        for (IIOObject iisObject : list) {
            if (iisObject.key1 == key1 && iisObject.key2 == key2) {
                return iisObject.o;
            }
        }
        return null;
    }*//*


    int length() {
        return list.size();
    }
}

class IIOObject {
    private int key1, key2;
    private Object o;

    IIOObject(int key1, int key2, Object o) {
        this.key1 = key1;
        this.key2 = key2;
        this.o = o;
    }
}*/
