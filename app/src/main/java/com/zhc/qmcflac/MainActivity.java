package com.zhc.qmcflac;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.zhc.qmcflac.qmcflac_Decode.Main;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    private EditText et;
    private String f = null;
    private File folder = null;
    private boolean isFolder = false;
    @SuppressLint("StaticFieldLeak")
    public static TextView tv;
    private boolean isDecoding = false;

    @TargetApi(Build.VERSION_CODES.DONUT)
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
                    String folder = data.getStringExtra("f");
                    System.out.println("folder = " + folder);
                    setF(folder);
                }
                break;
            case 2:
                if (data != null) {
                    try {
//                        String s = Objects.requireNonNull(data.getData()).getPath();
//                        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
                        Uri uri;
                        if ((uri = data.getData()) != null) {
                            String path = new GetPath().getPathFromUriOnKitKat(this, uri);
                            System.out.println("path = " + path);
                            setF(path);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        makeText(this, e.toString(), LENGTH_SHORT).show();
                        this.isDecoding = false;
                    }
                }
        }
    }

    private void D() {
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv);
        Button pF = findViewById(R.id.pF);
        this.et = findViewById(R.id.textView);
        pF.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, P0.class);
            startActivityForResult(intent, 1);
        });
        pF.setOnLongClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, 2);
            return true;
        });
        Button dB = findViewById(R.id.dB);
        dB.setOnClickListener(v -> {
            if (isDecoding) {
                makeText(this, "正在进行任务", LENGTH_SHORT).show();
                return;
            }
            if (isFolder) {
                File[] files = folder.listFiles();
                try {
                    ExecutorService es = Executors.newCachedThreadPool();
                    es.submit(() -> {
                        isDecoding = true;
                        int i = 1;
                        int length = files.length;
                        for (File file : files) {
                            if (file.isFile()) {
                                try {
                                    int finalI = i;
                                    runOnUiThread(() -> et.setText(String.format(getResources().getString(R.string.tv), finalI + " of " + length + ": " + file.getName())));
                                    File x = x(file);
                                    if (x != null) {
//                                        new Main().Do_Decode(file, x);
                                        new Main().JNI_Decode(file.getCanonicalPath(), x.getCanonicalPath());
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            ++i;
                        }
                        runOnUiThread(() -> {
                            tv.setText(String.format(getResources().getString(R.string.tv), "100%"));
                            makeText(this, "所有文件解码完成！", LENGTH_SHORT).show();
                        });
                    });
                    es.shutdown();
                    isDecoding = false;
                    isFolder = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> makeText(this, e.toString(), LENGTH_SHORT).show());
                    isDecoding = false;
                    isFolder = false;
                }
            } else {
                File dF = null;
                try {
//                for (int i = f.length() - 1; i > 0; --i) {
                    /*int i1 = f.lastIndexOf('.');
                    dF = new File(f.substring(0, i1 == -1 ? f.length() : i1) + ".flac");*/
                    dF = x(new File(f));
//                }
                } catch (Exception e) {
                    e.printStackTrace();
                    makeText(this, e.toString(), LENGTH_SHORT).show();
                    this.isDecoding = false;
                }
                try {
                    if (dF == null) {
                        runOnUiThread(() -> makeText(this, "不是qmcflac或qmc0", LENGTH_SHORT).show());
                        return;
                    }
                    File finalDF = dF;
                    File finalDF1 = dF;
                    new Thread(() -> {
                        try {
                            isDecoding = true;
                            try {
//                                new Main().Do_Decode(new File(f), Objects.requireNonNull(finalDF));
                                new Main().JNI_Decode(f, finalDF.getCanonicalPath());
                            } catch (IOException e) {
                                e.printStackTrace();
                                makeText(this, e.toString(), LENGTH_SHORT).show();
                                this.isDecoding = false;
                            }
                            isDecoding = false;
                            if (Objects.requireNonNull(finalDF1).exists() && finalDF1.length() > 0) {
                                runOnUiThread(() -> {
                                    tv.setText(String.format(getResources().getString(R.string.tv), "100%"));
                                    makeText(this, "解码完成", LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            makeText(this, e.toString(), LENGTH_SHORT).show();
                            this.isDecoding = false;
                            this.isFolder = false;
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    makeText(this, e.toString(), LENGTH_SHORT).show();
                    this.isDecoding = false;
                    this.isFolder = false;
                }
            }
        });
    }

    private void setF(String s) {
        File u = new File(s);
        if (u.isFile()) {
            try {
                this.f = u.getCanonicalPath();
                runOnUiThread(() -> this.et.setText(String.format(getResources().getString(R.string.tv), this.f)));
            } catch (IOException e) {
                e.printStackTrace();
                makeText(this, e.toString(), LENGTH_SHORT).show();
                this.isDecoding = false;
            }
        } else {
            isFolder = true;
            this.folder = new File(s);
            runOnUiThread(() -> et.setText(String.format(getResources().getString(R.string.tv), s)));
        }
    }

    private File x(File file) {
        String name = file.getName();
        String p = file.getParent();
        int index = name.lastIndexOf('.');
        String name_no_x = name.substring(0, index);
        String x = name.substring(index + 1);
        switch (x.toLowerCase()) {
            case "qmc0":
                return new File(p + "/" + name_no_x + ".mp3");
            case "qmcflac":
                return new File(p + "/" + name_no_x + ".flac");
        }
        return null;
    }
}
