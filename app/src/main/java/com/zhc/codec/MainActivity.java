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
import android.view.View;
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
    private Main main_o = new Main();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 0);
        } else {
            D();
        }
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
//                        String s = Objects.requireNonNull(data.getData()).getPath();
//                        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
//                        Uri uri;
//                        if ((uri = data.getData()) != null) {
//                            String path = new GetPath().getPathFromUriOnKitKat(this, uri);
                        String path = data.getStringExtra("result");
                        System.out.println("path = " + path);
                        setF(path);
//                        }

                    } catch (Exception e) {
                        main_o.showException(e, MainActivity.this);
                        reset();
                        runOnUiThread(() -> dB.setVisibility(VISIBLE));
                    }
                }
        }
    }

    private void D() {
        setContentView(R.layout.activity_main);
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
        this.dB = findViewById(R.id.dB);
        dB.setOnClickListener(v -> {
            dB.setVisibility(INVISIBLE);
            if (isDecoding) {
                makeText(this, "正在进行任务", LENGTH_SHORT).show();
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
                                        File x = x(file, this.dT);
                                        if (x != null) {
                                            if (file.length() != 0L)
                                                if (new Main().JNI_Decode(file.getCanonicalPath(), x.getCanonicalPath(), this.dT, tv) == -1)
                                                    runOnUiThread(() -> makeText(this, "打开文件错误", LENGTH_SHORT).show());
                                        }
                                    } catch (IOException e) {
                                        main_o.showException(e, MainActivity.this);
                                        reset();
                                        runOnUiThread(() -> dB.setVisibility(VISIBLE));
                                    }
                                }
                                ++i;
                            }
                            runOnUiThread(() -> {
                                tv.setText(String.format(getResources().getString(R.string.tv), "100%"));
                                makeText(this, "所有文件解码完成！", LENGTH_SHORT).show();
                                this.folder = null;
                                this.f = null;
                                runOnUiThread(() -> dB.setVisibility(VISIBLE));
                                mainTv.setText("");
                                reset();
                            });
                        } catch (Exception e) {
                            main_o.showException(e, MainActivity.this);
                            runOnUiThread(() -> {
                                makeText(this, e.toString(), LENGTH_SHORT).show();
                                dB.setVisibility(VISIBLE);
                                reset();
                            });
                        }
                    }).start();
                    reset();
                } catch (Exception e) {
                    main_o.showException(e, MainActivity.this);
                    reset();
                    runOnUiThread(() -> dB.setVisibility(VISIBLE));
                }
            } else {
                File dF = null;
                try {
//                for (int i = f.length() - 1; i > 0; --i) {
                    /*int i1 = f.lastIndexOf('.');
                    dF = new File(f.substring(0, i1 == -1 ? f.length() : i1) + ".flac");*/
                    dF = x(new File(f), this.dT);
//                }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!(e instanceof NullPointerException)) makeText(this, e.toString(), LENGTH_SHORT).show();
                    reset();
                    runOnUiThread(() -> dB.setVisibility(VISIBLE));
                }
                try {
                    if (dF == null) {
                        runOnUiThread(() -> {
                            makeText(this, "格式不正确", LENGTH_SHORT).show();
                            runOnUiThread(() -> dB.setVisibility(VISIBLE));
                        });
                        return;
                    }
                    File finalDF = dF;
                    new Thread(() -> {
                        boolean size0 = false;
                        try {
                            isDecoding = true;
                            try {
                                size0 = new File(f).length() == 0L;
                                int status = new Main().JNI_Decode(f, finalDF.getCanonicalPath(), this.dT, tv);
                                if (!size0 && (status == -1 || status == 255)) {
                                    runOnUiThread(() -> makeText(this, "打开文件错误", LENGTH_SHORT).show());
                                }
                                if (status == 1) {
                                    runOnUiThread(() -> makeText(this, "内部错误：如寻找key失败。。。", LENGTH_SHORT).show());
                                }
                            } catch (IOException e) {
                                main_o.showException(e, MainActivity.this);
                                reset();
                                runOnUiThread(() -> dB.setVisibility(VISIBLE));
                            }
                            isDecoding = false;
                            if (!size0 && Objects.requireNonNull(finalDF).exists() && finalDF.length() > 0) {
                                runOnUiThread(() -> {
                                    tv.setText(String.format(getResources().getString(R.string.tv), "100%"));
                                    makeText(this, "解码完成", LENGTH_SHORT).show();
                                    reset();
                                });
                            }
                            if (size0) runOnUiThread(() -> makeText(this, "为空文件", LENGTH_SHORT).show());
                            runOnUiThread(() -> dB.setVisibility(VISIBLE));
                        } catch (Exception e) {
                            main_o.showException(e, MainActivity.this);
                            reset();
                            runOnUiThread(() -> dB.setVisibility(VISIBLE));
                        }
                    }).start();
                } catch (Exception e) {
                    main_o.showException(e, MainActivity.this);
                    reset();
                    runOnUiThread(() -> dB.setVisibility(VISIBLE));
                }
            }
        });
        setTSpinner(new String[]{
                "QQMusic-qmc",
                "KwMusic-kwm"
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
                main_o.showException(e, MainActivity.this);
                reset();
                runOnUiThread(() -> dB.setVisibility(VISIBLE));
            }
        } else {
            isFolder = true;
            this.folder = new File(s);
            runOnUiThread(() -> mainTv.setText(String.format(getResources().getString(R.string.tv), s)));
        }
    }

    private File x(File file, int dT) {
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
                            return new File(p + "/" + name_no_x + ".mp3");
                        case "codec":
                            return new File(p + "/" + name_no_x + ".flac");
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
                return new File(r);
        }
        return null;
    }

    private void reset() {
        this.isDecoding = false;
        this.isFolder = false;
    }

    private void setTSpinner(String[] stringList) {
        Spinner dT = findViewById(R.id.dT);
        SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item
                , stringList);
        dT.setAdapter(adapter);
        dT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.dT = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}