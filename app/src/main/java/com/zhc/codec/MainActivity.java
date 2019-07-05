package com.zhc.codec;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import filepicker.Picker;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

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
    private boolean isDecoding = false;
    private Button dB = null;
    private int dT = 0;//qmc
    private Picker picker_o = new Picker();
    private Toast toasting = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        creat();
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
        }
    }

    private void D() {
        setContentView(R.layout.main_ll);
        resetBtn();
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
//        this.dB = findViewById(R.id.dB);
//        setDBOnClickEvent(this.dB);
        setTSpinner(new String[]{
                "QQMusic-qmc",
                "KwMusic-kwm",
                "Base128"
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
    private String x(File file, int dT) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        String name_no_x = name.substring(0, index);
        String p = file.getParent();
        String x = name.substring(index + 1).toLowerCase();
        switch (dT) {
            case 0:
                try {
                    switch (x) {
                        case "qmc0":
                            return p + "/" + name_no_x + ".mp3";
                        case "qmcflac":
                            return p + "/" + name_no_x + ".flac";
                    }
                } catch (StringIndexOutOfBoundsException ignored) {
                }
                break;
            case 1:
                if (!(x.equals("kwm") | x.equals("kwd"))) return null;
                String r;
                if (name.matches(".*\\..*")) {
                    r = p + "/" + name_no_x + ".flac";
                } else {
                    r = p + "/" + name + ".flac";
                }
                return r;
            case 21:
                return p + "/" + name + ".base128e";
            case 22:
                return p + "/" + name + ".base128d";
        }
        return null;
    }

    private void reset() {
        this.isDecoding = false;
        this.isFolder = false;
    }

    private void setTSpinner(String[] data) {
        Spinner dT = findViewById(R.id.dT);
        SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item
                , data);
        dT.setAdapter(adapter);
        dT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.dT = position;
                if (data[position].equals("Base128")) {
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
            if (isDecoding) {
                makeText(this, R.string.have_task, LENGTH_SHORT).show();
                return;
            }
            if (isFolder) {
                File[] files = folder.listFiles();
                try {
                    new Thread(() -> {
                        try {
                            isDecoding = true;
                            int i = 1;
                            int length = files.length;
                            for (File file : files) {
                                if (file.isFile()) {
                                    try {
                                        int finalI = i;
                                        runOnUiThread(() -> mainTv.setText(String.format(getResources().getString(R.string.tv), finalI + " of " + length + ": " + file.getName())));
                                        String x = x(file, o == 1 ? this.dT : dT);
                                        if (x != null) {
                                            String fPath = file.getCanonicalPath();
                                            if (file.length() != 0L)
                                                if (new Main().JNI_Decode(fPath, x, o == 1 ? this.dT : dT, tv) == -1)
                                                    runOnUiThread(() -> makeText(this, R.string.fopen_error, LENGTH_SHORT).show());
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
                                tv.setText(R.string.percent_100);
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
                    dF = x(new File(f), o == 1 ? this.dT : dT);
//                }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!(e instanceof NullPointerException)) makeText(this, e.toString(), LENGTH_SHORT).show();
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
                            isDecoding = true;
                            try {
                                size0 = new File(f).length() == 0L;
                                int status = new Main().JNI_Decode(f, finalDF, o == 1 ? this.dT : dT, tv);
                                if (!size0 && (status == -1 || status == 255)) {
                                    runOnUiThread(() -> makeText(this, R.string.fopen_error, LENGTH_SHORT).show());
                                }
                                if (status == 1) {
                                    runOnUiThread(() -> makeText(this, R.string.native_error, LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                picker_o.showException(e, MainActivity.this);
                                reset();
                                allButtonsAction(o, buttons, VISIBLE);
                            }
                            isDecoding = false;
                            if (!size0 && new File(finalDF).exists() && finalDF.length() > 0) {
                                runOnUiThread(() -> {
                                    tv.setText(R.string.percent_100);
                                    makeText(this, o == 1 ? R.string.decode_done : (dT == 21 ? R.string.encode_done : R.string.decode_done), LENGTH_SHORT).show();
                                    reset();
                                });
                            }
                            if (size0) runOnUiThread(() -> makeText(this, R.string.null_file, LENGTH_SHORT).show());
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
}