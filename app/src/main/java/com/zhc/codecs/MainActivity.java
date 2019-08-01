package com.zhc.codecs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import filepicker.Picker;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {
    private TextView mainTv;
    private String f = null;
    private File folder = null;
    private boolean isFolder = false;
    private TextView tv;
    private boolean isRunning = false;
    private Button dB = null;
    private int dT = 0;//qmc
    private Picker picker_o;
    private Toast toasting = null;
    List<String> spinnerData;
    private CountDownLatch latch;
    private File file;
    private String jsonText;
    private List<List<String>> savedConfig;
    private List<String> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.creat();
    }

    @SuppressWarnings("WeakerAccess")
    public MainActivity() {
        System.out.println("new MainActivity");
        this.picker_o = new Picker();
        this.dataList = new ArrayList<>();
        {
            this.dataList.add("QQMusic-qmc");
            this.dataList.add("KwMusic-kwm");
            this.dataList.add("Base128编码");
            this.dataList.add("Base128解码");
        }
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults[0] == 0) {
                D();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (data != null) {
                    String folder = data.getStringExtra("result");
                    System.out.println("folder = " + folder);
                    setF(folder);
                }
                break;
            case 2:
                if (data != null) {
                    try {
                        String path = data.getStringExtra("result");
                        System.out.println("path = " + path);
                        setF(path);
//                        }

                    } catch (Exception e) {
                        picker_o.showException(e, MainActivity.this);
                        reset();
                        allButtonsAction(1, null, VISIBLE);
                    }
                }
                break;
            case 3:
                if (data != null) {
                    try {
                        this.savedConfig = this.solveJSON(data.getStringExtra("result"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.runOnUiThread(() -> makeText(this, this.getString(R.string.json_solve_error) + e.toString(), LENGTH_SHORT).show());
                    }
//                    this.creat();
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.fab), R.string.saving_success, Snackbar.LENGTH_SHORT);
                    snackbar.setAction("×", v -> snackbar.dismiss()).show();
                }
                break;
        }
    }

    private void D() {
        setContentView(R.layout.activity_main);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putStringArrayListExtra("options", (ArrayList<String>) this.dataList);
            intent.putExtra("jsonText", this.jsonText);
            intent.setClass(this, Settings.class);
            startActivityForResult(intent, 3);
        });
        tv = findViewById(R.id.tv);
        Button pF = findViewById(R.id.pF);
        this.mainTv = findViewById(R.id.textView);
        pF.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("option", Picker.PICK_FILE);
            intent.setClass(this, Picker.class);
            startActivityForResult(intent, 1);
        });
        pF.setOnLongClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("option", Picker.PICK_FOLDER);
            intent.setClass(this, Picker.class);
            startActivityForResult(intent, 2);
            return true;
        });
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            spinnerData = new ArrayList<>();
            spinnerData.add("QQMusic-qmc");
            spinnerData.add("KwMusic-kwm");
            spinnerData.add("Base128");
            runOnUiThread(this::resetBtn);
//        this.dB = findViewById(R.id.dB);
//        setDBOnClickEvent(this.dB);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> setTSpinner(this.spinnerData));
        });

        es.execute(() -> {
            this.savedConfig = this.loadSavedConfig();
            latch.countDown();
        });
    }

    private void setF(String s) {
        File u = new File(s);
        if (u.isFile()) {
            reset();
            try {
                this.f = u.getCanonicalPath();
                runOnUiThread(() -> this.mainTv.setText(String.format(getResources().getString(R.string.tv), this.f)));
            } catch (IOException e) {
                picker_o.showException(e, MainActivity.this);
                reset();
                allButtonsAction(1, null, VISIBLE);
            }
        } else {
            isFolder = true;
            this.folder = new File(s);
            runOnUiThread(() -> mainTv.setText(String.format(getResources().getString(R.string.tv), s)));
        }
    }

    /**
     * get dest file name
     *
     * @param file source file
     * @param dT   0: qmc0/qmcflac
     *             1: kwm
     *             2[0-9]{1} : Base128 {
     *             21: Base128 encode
     *             22: Base128 decode
     *             }
     * @return dest file
     */
    @SuppressWarnings("SpellCheckingInspection")
    private String x(File file, int dT, List<List<String>> conf) {
        String name = file.getName();
        String name_no_x, x = "";
        if (name.matches(".*\\..*")) {
            int index = name.lastIndexOf('.');
            name_no_x = name.substring(0, index);
            x = name.substring(index + 1).toLowerCase();
        } else {
            name_no_x = name;
        }
        String p = file.getParent();
        String[] srcExtensions;
        String[] dstExtensions;
        try {
            switch (dT) {
                case 0:
                    srcExtensions = conf.get(0).get(0).split("\\|");
                    dstExtensions = conf.get(0).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.fab), R.string.expression_error, Snackbar.LENGTH_SHORT);
                        snackbar.setAction("×", v -> snackbar.dismiss()).show();
                        try {
                            switch (x) {
                                case "qmc0":
                                    return p + "/" + name_no_x + ".mp3";
                                case "qmcflac":
                                    return p + "/" + name_no_x + ".flac";
                            }

                        } catch (StringIndexOutOfBoundsException ignored) {
                        }
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x);
                case 1:
                    srcExtensions = conf.get(1).get(0).split("\\|");
                    dstExtensions = conf.get(1).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.fab), R.string.expression_error, Snackbar.LENGTH_SHORT);
                        snackbar.setAction("×", v -> snackbar.dismiss()).show();
                        if (!(x.equals("kwm") | x.equals("kwd"))) return null;
                        String r;
                        if (name.matches(".*\\..*")) {
                            r = p + "/" + name_no_x + ".flac";
                        } else {
                            r = p + "/" + name + ".flac";
                        }
                        return r;
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x);
                case 21:
                    srcExtensions = conf.get(1).get(0).split("\\|");
                    dstExtensions = conf.get(1).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.fab), R.string.expression_error, Snackbar.LENGTH_SHORT);
                        snackbar.setAction("×", v -> snackbar.dismiss()).show();
                        return p + "/" + name_no_x + "." + "base128e";
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x);
                case 22:
                    return p + "/" + name_no_x + "." + conf.get(3).get(1);
            }
        } catch (Exception e) {
            makeText(this, e.toString(), LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return null;
    }

    private void reset() {
        this.isRunning = false;
        this.isFolder = false;
    }

    private void setTSpinner(List<String> data) {
        Spinner dT = findViewById(R.id.dT);
        SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item
                , data);
        dT.setAdapter(adapter);
        dT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.dT = position;
                if (data.get(position).equals("Base128")) {
                    LinearLayout ll = findViewById(R.id.fl);
                    Button[] base128_btn = new Button[]{
                            new Button(MainActivity.this), new Button(MainActivity.this)
                    };
                    LinearLayout.LayoutParams[] ll_lp = new LinearLayout.LayoutParams[]{
                            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1),
                            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    };
                    ll.removeAllViews();
                    setDBOnClickEvent(null, 2, base128_btn);
                    base128_btn[0].setText(R.string.encode);
                    base128_btn[1].setText(R.string.decode);
                    LinearLayout[] linearLayouts = new LinearLayout[]{
                            new LinearLayout(MainActivity.this),
                            new LinearLayout(MainActivity.this)
                    };
                    for (int i = 0; i < 2; i++) {
                        base128_btn[i].setLayoutParams(ll_lp[i]);
                        ll_lp[i].gravity = Gravity.CENTER;
                        linearLayouts[i].setLayoutParams(ll_lp[i]);
                        linearLayouts[i].addView(base128_btn[i]);
                        ll.addView(linearLayouts[i]);
                    }
                } else {
                    resetBtn();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void creat() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 0);
        } else {
            D();
        }
    }

    /**
     * setOnClickListener
     *
     * @param do_button btn
     * @param o         decode 1
     *                  encode and decode 2
     */
    private void setDBOnClickEvent(@Nullable Button do_button, int o, @Nullable Button[] buttons) {
        View.OnClickListener v = getV(o, 0, null);
        if (o == 2 && buttons != null) {
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setOnClickListener(getV(o, 21 + i, buttons));
            }
            return;
        }
        Objects.requireNonNull(do_button).setOnClickListener(v);
    }

    private void resetBtn() {
        Button btn = new Button(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(lp);
        btn.setText(R.string.decode);
        btn.setId(R.id.dB);
        this.dB = btn;
        MainActivity.this.setDBOnClickEvent(btn, 1, null);
        LinearLayout ll = findViewById(R.id.fl);
        ll.removeAllViews();
        ll.addView(btn);
    }

    /**
     * getV
     *
     * @param o       decode 1
     *                encode and decode 2
     * @param dT      o:1 be ignored
     *                o:2 base128:
     *                21: encode
     *                22:decode
     * @param buttons o == 21 || o == 22: Base128 buttons
     * @return v
     */
    private View.OnClickListener getV(int o, int dT, @Nullable Button[] buttons) {
        return v -> {
            allButtonsAction(o, buttons, INVISIBLE);
            if (isRunning) {
                makeText(this, R.string.have_task, LENGTH_SHORT).show();
                return;
            }
            //noinspection SpellCheckingInspection
            Toast fopenErrorToast = makeText(this, R.string.fopen_error, LENGTH_SHORT);
            if (isFolder) {
                File[] files = folder.listFiles();
                try {
                    new Thread(() -> {
                        try {
                            isRunning = true;
                            int i = 1;
                            int length = files.length;
                            for (File file : files) {
                                if (file.isFile()) {
                                    try {
                                        int finalI = i;
                                        runOnUiThread(() -> mainTv.setText(String.format(getResources().getString(R.string.tv), finalI + " of " + length + ": " + file.getName())));
                                        String x = x(file, o == 1 ? this.dT : dT, this.savedConfig);
                                        if (x != null) {
                                            String fPath = file.getCanonicalPath();
                                            if (file.length() != 0L) {
                                                int status = new Main().JNI_Decode(fPath, x, o == 1 ? this.dT : dT, tv, MainActivity.this, 0);
                                                if (status == -1)
                                                    runOnUiThread(() -> {
                                                        fopenErrorToast.cancel();
                                                        fopenErrorToast.show();
                                                    });
                                            }
                                        }
                                    } catch (IOException e) {
                                        picker_o.showException(e, MainActivity.this);
                                        reset();
                                        allButtonsAction(o, buttons, VISIBLE);
                                    }
                                }
                                ++i;
                            }
                            runOnUiThread(() -> {
//                                tv.setText(R.string.percent_100);
                                makeText(this, o == 1 ? R.string.all_file_decoded_done : (dT == 21 ? R.string.all_file_encoded_done : R.string.all_file_decoded_done), LENGTH_SHORT).show();
                                this.folder = null;
                                this.f = null;
                                allButtonsAction(o, buttons, VISIBLE);
                                mainTv.setText(R.string.nul);
                                tv.setText(R.string.nul);
                                reset();
                            });
                        } catch (Exception e) {
                            picker_o.showException(e, MainActivity.this);
                            runOnUiThread(() -> {
                                makeText(this, e.toString(), LENGTH_SHORT).show();
                                dB.setVisibility(VISIBLE);
                                reset();
                            });
                        }
                    }).start();
                    reset();
                } catch (Exception e) {
                    picker_o.showException(e, MainActivity.this);
                    reset();
                    allButtonsAction(o, buttons, VISIBLE);
                }
            } else {
                String dF = null;
                try {
                    dF = x(new File(f), o == 1 ? this.dT : dT, this.savedConfig);
//                }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!(e instanceof NullPointerException))
                        makeText(this, e.toString(), LENGTH_SHORT).show();
                    reset();
                    allButtonsAction(o, buttons, VISIBLE);
                }
                try {
                    if (dF == null) {
                        runOnUiThread(() -> {
                            if (toasting != null) {
                                toasting.cancel();
                            }
                            toasting = makeText(this, R.string.incorrect_file_extension, LENGTH_SHORT);
                            toasting.show();
                            allButtonsAction(o, buttons, VISIBLE);
                        });
                        return;
                    }
                    String finalDF = dF;
                    new Thread(() -> {
                        boolean size0 = false;
                        try {
                            isRunning = true;
                            try {
                                size0 = new File(f).length() == 0L;
                                int status = 2;
                                if (size0)
                                    runOnUiThread(() -> {
                                        makeText(this, R.string.null_file, LENGTH_SHORT).show();
                                        allButtonsAction(o, buttons, VISIBLE);
                                    });
                                else
                                    status = new Main().JNI_Decode(f, finalDF, o == 1 ? this.dT : dT, tv, MainActivity.this, 0);
                                if (status == -1 || status == 255) {
                                    runOnUiThread(() -> {
                                        fopenErrorToast.cancel();
                                        fopenErrorToast.show();
                                    });
                                }
                                if (status == 1) {
                                    runOnUiThread(() -> makeText(this, R.string.native_error, LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                picker_o.showException(e, MainActivity.this);
                                reset();
                                allButtonsAction(o, buttons, VISIBLE);
                            }
                            isRunning = false;
                            if (!size0 && new File(finalDF).exists() && finalDF.length() > 0) {
                                runOnUiThread(() -> {
//                                    tv.setText(R.string.percent_100);
                                    makeText(this, o == 1 ? R.string.decode_done : (dT == 21 ? R.string.encode_done : R.string.decode_done), LENGTH_SHORT).show();
                                    reset();
                                });
                            }
                            allButtonsAction(o, buttons, VISIBLE);
                        } catch (Exception e) {
                            picker_o.showException(e, MainActivity.this);
                            reset();
                            allButtonsAction(o, buttons, VISIBLE);
                        }
                    }).start();
                } catch (Exception e) {
                    picker_o.showException(e, MainActivity.this);
                    reset();
                    allButtonsAction(o, buttons, VISIBLE);
                }
            }
        };
    }

    /**
     * @param o       1 2
     * @param buttons buttons
     * @param t       appear: 0x0
     *                disappear: 0x4
     */
    private void allButtonsAction(int o, @Nullable Button[] buttons, int t) {
        if (o == 1) runOnUiThread(() -> dB.setVisibility(t));
        else if (buttons != null) runOnUiThread(() -> {
            for (Button button : buttons) {
                button.setVisibility(t);
            }
        });
    }

    /**
     * onConfigurationChanged
     * the package:android.content.res.Configuration.
     *
     * @param newConfig, The new device configuration.
     *                   当设备配置信息有改动（比如屏幕方向的改变，实体键盘的推开或合上等）时，
     *                   并且如果此时有activity正在运行，系统会调用这个函数。
     *                   注意：onConfigurationChanged只会监测应用程序在AndroidManifest.xml中通过
     *                   android:configChanges="xxxx"指定的配置类型的改动；
     *                   而对于其他配置的更改，则系统会onDestroy()当前Activity，然后重启一个新的Activity实例。
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        String TAG = "onConfigurationChanged";
        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            //当前为横屏， 在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 当前为横屏");
        } else if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            //当前为竖屏， 在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 当前为竖屏");
        }
        //检测实体键盘的状态：推出或者合上
        if (newConfig.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //实体键盘处于推出状态，在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 实体键盘处于推出状态");
        } else if (newConfig.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_YES) {
            //实体键盘处于合上状态，在此处添加额外的处理代码
            Log.d(TAG, "onConfigurationChanged: 实体键盘处于合上状态");
        }
    }

    private List<List<String>> loadSavedConfig() {
        List<List<String>> saved = new ArrayList<>();
        try {
            this.file = getFile();
            if (!file.exists()) System.out.println("file.createNewFile() = " + file.createNewFile());
            InputStream is = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String r = br.readLine();
            while (r != null) {
                sb.append(r);
                r = br.readLine();
            }
            br.close();
            is.close();
            System.out.println("sb.toString() = " + sb.toString());
            saved = this.solveJSON(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            this.runOnUiThread(() -> makeText(this, this.getString(R.string.json_solve_error) + e.toString(), LENGTH_SHORT).show());
        }
        return saved;
    }

    List<List<String>> solveJSON(String jsonString) throws JSONException {
        List<List<String>> saved = new ArrayList<>();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
            OutputStream os;
            try {
                this.file = getFile();
                os = new FileOutputStream(file, false);
                os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (jsonObject != null) {
            saved = new ArrayList<>();
            List<String> childStr;
            for (String s : this.dataList) {
                childStr = new ArrayList<>();
                JSONObject child = (JSONObject) jsonObject.get(s);
                childStr.add(child.getString("sourceExtension"));
                childStr.add(child.getString("destExtension"));
                saved.add(childStr);
            }
            this.jsonText = jsonObject.toString();
        }
        return saved;
    }

    public File getFile() {
        File f = null;
        try {
            File storageDirectory = Environment.getExternalStorageDirectory();
            File canonicalFile = storageDirectory.getCanonicalFile();
            File d = new File(canonicalFile + "/codecsApp");
            if (!d.exists()) System.out.println("d.mkdir() = " + d.mkdir());
            f = new File(canonicalFile + "/codecsApp/config.json");
        } catch (IOException e) {
            new Picker().showException(e, this);
        }
        return f;
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if (this.isRunning) this.moveTaskToBack(false);
        else finish();
    }

    private String conf_getX(String[] srcExtensions, String[] dstExtensions, String p, String name_no_x) {
        for (int i = 0; i < srcExtensions.length; i++) {
            if (srcExtensions[i].equalsIgnoreCase(dstExtensions[i])) {
                return p + "/" + name_no_x + "." + dstExtensions[i];
            }
        }
        return null;
    }
}