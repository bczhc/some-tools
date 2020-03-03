package pers.zhc.tools.codecs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.MainActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.Picker;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.tools.utils.ToastUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static pers.zhc.tools.utils.Common.showException;

public class CodecsActivity extends BaseActivity {
    private final CountDownLatch latch;
    String[] jsonData = null;
    private TextView mainTv;
    private String f = null;
    private File folder = null;
    private boolean isFolder = false;
    private TextView tv;
    private boolean isRunning = false;
    private Button dB = null;
    private int dT = 0;//qmc
    private String jsonText;
    private List<List<String>> savedConfig;
    private File file = null;
    private String[] spinnerData = null;

    @SuppressWarnings("WeakerAccess")
    public CodecsActivity() {
        System.out.println("new CodecsActivity");
        this.latch = new CountDownLatch(1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PermissionRequester(this::D).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 13) {
            if (grantResults[0] == 0) {
                D();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_1:
                if (data != null) {
                    String folder = data.getStringExtra("result");
                    System.out.println("folder = " + folder);
                    setF(folder);
                }
                break;
            case RequestCode.START_ACTIVITY_2:
                if (data != null) {
                    try {
                        String path = data.getStringExtra("result");
                        System.out.println("path = " + path);
                        setF(path);

                    } catch (Exception e) {
                        showException(e, CodecsActivity.this);
                        reset();
                        allButtonsAction(1, null, VISIBLE);
                    }
                }
                break;
            case RequestCode.START_ACTIVITY_3:
                if (data != null) {
                    try {
                        this.savedConfig = this.solveJSON(data.getStringExtra("result"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.runOnUiThread(() -> toast(this.getString(R.string.json_solve_error) + e.toString()));
                    }
//                    this.creat();
                    ToastUtils.show(this, R.string.saving_success);
                }
                break;
        }
    }

    @SuppressLint("ShowToast")
    private void D() {
        CountDownLatch latch1 = new CountDownLatch(1);
        setContentView(R.layout.codecs_activity);
        new Thread(() -> {
            this.spinnerData = this.getResources().getStringArray(R.array.spinner_data);
            this.jsonData = this.getResources().getStringArray(R.array.json);
            this.file = getFile(this);
            latch1.countDown();
        }).start();
        Button sBtn = findViewById(R.id.setting_btn);
        sBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("jsonText", this.jsonText);
            intent.putExtra("file", this.file);
            intent.setClass(this, Settings.class);
            startActivityForResult(intent, RequestCode.START_ACTIVITY_3);
        });
        sBtn.setOnLongClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
            return true;
        });
        tv = findViewById(R.id.tv);
        Button pF = findViewById(R.id.pF);
        this.mainTv = findViewById(R.id.textView);
        pF.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("option", Picker.PICK_FILE);
            intent.setClass(this, Picker.class);
            startActivityForResult(intent, RequestCode.START_ACTIVITY_1);
            overridePendingTransition(R.anim.in_left_and_bottom, 0);
        });
        pF.setOnLongClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("option", Picker.PICK_FOLDER);
            intent.setClass(this, Picker.class);
            startActivityForResult(intent, RequestCode.START_ACTIVITY_2);
            overridePendingTransition(R.anim.in_left_and_bottom, 0);
            return true;
        });
        try {
            latch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            runOnUiThread(this::resetBtn);
//        this.dB = findViewById(R.id.dB);
//        setDBOnClickEvent(this.dB);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(this::setTSpinner);
        });

        es.execute(() -> {
            this.savedConfig = this.loadSavedConfig();
            latch.countDown();
        });
        es.shutdown();
    }

    private void setF(String s) {
        File u = new File(s);
        if (u.isFile()) {
            reset();
            try {
                this.f = u.getCanonicalPath();
                runOnUiThread(() -> this.mainTv.setText(String.format(getResources().getString(R.string.tv), this.f)));
            } catch (IOException e) {
                showException(e, CodecsActivity.this);
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
                        ToastUtils.show(this, R.string.dismiss_x);
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
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x, x);
                case 1:
                    srcExtensions = conf.get(1).get(0).split("\\|");
                    dstExtensions = conf.get(1).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        ToastUtils.show(this, R.string.dismiss_x);
                        if (!(x.equals("kwm") | x.equals("kwd"))) return null;
                        String r;
                        if (name.matches(".*\\..*")) {
                            r = p + "/" + name_no_x + ".flac";
                        } else {
                            r = p + "/" + name + ".flac";
                        }
                        return r;
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x, x);
                case 21:
                    srcExtensions = conf.get(2).get(0).split("\\|");
                    dstExtensions = conf.get(2).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        ToastUtils.show(this, R.string.dismiss_x);
                        return p + "/" + name_no_x + "." + "base128e";
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x, x);
                case 22:
                    srcExtensions = conf.get(3).get(0).split("\\|");
                    dstExtensions = conf.get(3).get(1).split("\\|");
                    if (srcExtensions.length != dstExtensions.length) {
                        ToastUtils.show(this, R.string.dismiss_x);
                        return p + "/" + name_no_x + "." + "base128d";
                    }
                    return conf_getX(srcExtensions, dstExtensions, p, name_no_x, x);
            }
        } catch (Exception e) {
            toast(e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private void reset() {
        this.isRunning = false;
        this.isFolder = false;
    }

    private void setTSpinner() {
        Spinner dT = findViewById(R.id.dT);
        SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.adapter_layout
                , this.spinnerData);
        dT.setAdapter(adapter);
        dT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CodecsActivity.this.dT = position;
                if (CodecsActivity.this.spinnerData[position].equals("Base128")) {
                    LinearLayout ll = findViewById(R.id.fl);
                    Button[] base128_btn = new Button[]{
                            new Button(CodecsActivity.this), new Button(CodecsActivity.this)
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
                            new LinearLayout(CodecsActivity.this),
                            new LinearLayout(CodecsActivity.this)
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
        Button btn = new Button(CodecsActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(lp);
        btn.setText(R.string.decode);
        btn.setId(R.id.dB);
        this.dB = btn;
        CodecsActivity.this.setDBOnClickEvent(btn, 1, null);
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
                toast(R.string.have_task);
                return;
            }
            if (isFolder) {
                File[] files = folder.listFiles();
                if (files == null) {
                    files = new File[0];
                }
                try {
                    File[] finalFiles = files;
                    new Thread(() -> {
                        try {
                            isRunning = true;
                            int i = 1;
                            int length = finalFiles.length;
                            for (File file : finalFiles) {
                                if (file.isFile()) {
                                    try {
                                        int finalI = i;
                                        runOnUiThread(() -> mainTv.setText(String.format(getResources().getString(R.string.tv), finalI + " of " + length + ": " + file.getName())));
                                        String x = x(file, o == 1 ? this.dT : dT, this.savedConfig);
                                        if (x != null) {
                                            String fPath = file.getCanonicalPath();
                                            if (file.length() != 0L) {
                                                int status = new JNIMain().JNI_Decode(fPath, x, o == 1 ? this.dT : dT, tv, CodecsActivity.this, this.savedConfig);
                                                if (status == -1)
                                                    runOnUiThread(() -> toast(R.string.fopen_error));
                                            }
                                        }
                                    } catch (IOException e) {
                                        showException(e, CodecsActivity.this);
                                        reset();
                                        allButtonsAction(o, buttons, VISIBLE);
                                    }
                                }
                                ++i;
                            }
                            runOnUiThread(() -> {
//                                tv.setText(R.string.percent_100);
                                toast(o == 1 ? R.string.all_file_decoded_done : (dT == 21 ? R.string.all_file_encoded_done : R.string.all_file_decoded_done));
                                allButtonsAction(o, buttons, VISIBLE);
                                String folder = this.folder.toString();
                                this.mainTv.setText(String.format(getString(R.string.tv), folder));
                                this.folder = null;
                                this.f = null;
                                tv.setText(R.string.nul);
                                reset();
                                setF(folder);
                            });
                        } catch (Exception e) {
                            showException(e, CodecsActivity.this);
                            runOnUiThread(() -> {
                                toast(e.toString());
                                dB.setVisibility(VISIBLE);
                                reset();
                            });
                        }
                    }).start();
                    reset();
                } catch (Exception e) {
                    showException(e, CodecsActivity.this);
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
                        toast(e.toString());
                    reset();
                    allButtonsAction(o, buttons, VISIBLE);
                }
                try {
                    if (dF == null) {
                        runOnUiThread(() -> {
                            toast(R.string.incorrect_file_extension);
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
                                        toast(R.string.null_file);
                                        allButtonsAction(o, buttons, VISIBLE);
                                    });
                                else
                                    status = new JNIMain().JNI_Decode(f, finalDF, o == 1 ? this.dT : dT, tv, CodecsActivity.this, savedConfig);
                                if (status == -1 || status == 255) {
                                    runOnUiThread(() -> toast(R.string.fopen_error));
                                }
                                if (status == 1) {
                                    runOnUiThread(() -> toast(R.string.native_error));
                                }
                            } catch (Exception e) {
                                showException(e, CodecsActivity.this);
                                reset();
                                allButtonsAction(o, buttons, VISIBLE);
                            }
                            isRunning = false;
                            if (!size0 && new File(finalDF).exists() && finalDF.length() > 0) {
                                runOnUiThread(() -> {
//                                    tv.setText(R.string.percent_100);
                                    toast(o == 1 ? R.string.decode_done : (dT == 21 ? R.string.encode_done : R.string.decode_done));
                                    reset();
                                });
                            }
                            allButtonsAction(o, buttons, VISIBLE);
                        } catch (Exception e) {
                            showException(e, CodecsActivity.this);
                            reset();
                            allButtonsAction(o, buttons, VISIBLE);
                        }
                    }).start();
                } catch (Exception e) {
                    showException(e, CodecsActivity.this);
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

    private List<List<String>> loadSavedConfig() {
        List<List<String>> saved = new ArrayList<>();
        try {
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
            this.runOnUiThread(() -> toast(this.getString(R.string.json_solve_error) + e.toString()));
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
                os = new FileOutputStream(file, false);
                os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (jsonObject != null) {
            saved = new ArrayList<>();
            List<String> childStr;

            for (String x : this.jsonData) {
                childStr = new ArrayList<>();
                JSONObject child = (JSONObject) jsonObject.get(x);
                childStr.add(child.getString("sourceExtension"));
                childStr.add(child.getString("destExtension"));
                childStr.add(child.getString("deleteOldFile"));
                saved.add(childStr);
            }
            this.jsonText = jsonObject.toString();
        }
        return saved;
    }

    private File getFile(Activity ctx) {
        File f = null;
        try {
            File storageDirectory = ctx.getFilesDir();
            if (!storageDirectory.exists()) {
                System.out.println("storageDirectory.mkdirs() = " + storageDirectory.mkdirs());
            }
            f = new File(storageDirectory + "/config.json");
        } catch (Exception e) {
            showException(e, ctx);
        }
        if (f == null) {
            toast(R.string.get_config_file_failed);
        }
        return f;
    }

    @Override
    public void onBackPressed() {
        if (this.isRunning) this.moveTaskToBack(false);
        else finish();
//        else super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    private String conf_getX(String[] srcExtensions, String[] dstExtensions, String p, String name_no_x, String x) {
        for (int i = 0; i < srcExtensions.length; i++) {
            if (srcExtensions[i].equalsIgnoreCase(x)) {
                return p + "/" + name_no_x + "." + dstExtensions[i];
            }
        }
        return null;
    }

    private void toast(@StringRes int strRes) {
        ToastUtils.show(this, strRes);
    }

    private void toast(CharSequence charSequence) {
        ToastUtils.show(this, charSequence);
    }
}