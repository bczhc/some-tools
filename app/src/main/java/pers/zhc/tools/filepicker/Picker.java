package pers.zhc.tools.filepicker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.*;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.PermissionRequester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static pers.zhc.tools.utils.Common.showException;

public class Picker extends AppCompatActivity {
    private Toast notHavePermissionAccessToast = null;
    @SuppressWarnings("unused")
    public static int PICK_FILE = 1;
    @SuppressWarnings("unused")
    public static int PICK_FOLDER = 2;
    private String resultString = "";
    private TextView pathView;
    private File currentPath;
    private LinearLayout ll;
    private LinearLayout.LayoutParams lp;
    private int grey = Color.parseColor("#DCDCDC");
    private int white = Color.WHITE;
    private final int[] justPicked = new int[]{-1};
    private int option = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PermissionRequester(this::D).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 33);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 33 && grantResults[0] == 0) D();
    }

    private void D() {
        Intent intent = getIntent();
        this.option = intent.getIntExtra("option", 0);
        String path = intent.getStringExtra("path");
        intent.getBooleanExtra("ioResult", false);
        setContentView(R.layout.file_picker_activity);
        this.lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        this.currentPath = path == null ? Environment.getExternalStorageDirectory() : new File(path);
//        this.currentPath = new File("/storage/emulated/0");
        lp.setMargins(2, 10, 10, 0);
        Button cancel = findViewById(R.id.cancel);
        Button ok = findViewById(R.id.pick);
        cancel.setOnClickListener(v -> {
            setResult(3, null);
            finish();
        });
        ok.setOnClickListener(v -> {
            Intent r = new Intent();
            switch (option) {
                case 1:
                    r.putExtra("result", resultString);
                    break;
                case 2:
                    String dir = null;
                    try {
                        dir = currentPath.getCanonicalPath();
                    } catch (IOException e) {
                        showException(e, this);
                    }
                    r.putExtra("result", dir);
                    break;
            }
            this.setResult(3, r);
            try {
                ioSetResult(r.getStringExtra("result"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        });
        this.pathView = findViewById(R.id.textView);
        this.pathView.setOnClickListener((v) -> {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            String s = pathView.getText().toString();
            et.setText(String.format("%s", s.equals("/storage/emulated") ? s + "/0" : s));
            et.setLayoutParams(lp);
            ad.setTitle(R.string.type_path)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        File f = new File(et.getText().toString());
                        if (f.isFile() && option == 1) {
                            Intent r = new Intent();
                            try {
                                r.putExtra("result", f.getCanonicalFile());
                            } catch (IOException e) {
                                showException(e, this);
                            }
                            this.setResult(3, r);
                            try {
                                ioSetResult(r.getStringExtra("result"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            finish();
                        } else {
                            File[] listFiles = f.listFiles();
                            this.currentPath = f;
                            fillViews(listFiles, lp, grey, justPicked, ll);
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .setView(et).show();
        });
        this.ll = findViewById(R.id.ll);
        new Thread(() -> {
            File[] listFiles = currentPath.listFiles();
            fillViews(listFiles, lp, grey, justPicked, ll);
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (currentPath.equals(new File("/"))) finish();
        else previous();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fillViews(File[] listFiles, LinearLayout.LayoutParams lp, int unselectedColor,
                           int[] justPicked, LinearLayout ll) {
        new Thread(() -> {
            justPicked[0] = -1;
            runOnUiThread(() -> {
                ll.removeAllViews();
                try {
                    pathView.setText(String.format("%s", currentPath.getCanonicalFile()));
                } catch (IOException e) {
                    showException(e, this);
                }
            });
            TextView[] textViews;
            int length = 0;
            try {
                length = listFiles.length;
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (notHavePermissionAccessToast != null) notHavePermissionAccessToast.cancel();
                    notHavePermissionAccessToast = Toast.makeText(this, R.string.no_access, Toast.LENGTH_SHORT);
                    notHavePermissionAccessToast.show();
                    /*Snackbar snackbar = Snackbar.make(this.ll, R.string.no_access, Snackbar.LENGTH_SHORT);
                    snackbar.setAction("×", v -> snackbar.dismiss()).show();*/
                });
                e.printStackTrace();
            }
            textViews = new TextView[length];
            switch (option) {
                case 1:
                    for (int i = 0; i < length; i++) {
                        final int finalI = i;
                        extractM1(listFiles, lp, unselectedColor, textViews, i);
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[finalI];
                            if (currentFile.isFile()) {
                                Drawable background = textViews[finalI].getBackground();
                                ColorDrawable colorDrawable = (ColorDrawable) background;
                                runOnUiThread(() -> {
                                    if (colorDrawable.getColor() == Color.GREEN) {
                                        textViews[finalI].setBackgroundColor(white);
                                    } else {
                                        try {
                                            if (justPicked[0] != -1)
                                                textViews[justPicked[0]].setBackgroundColor(white);
                                            textViews[finalI].setBackgroundColor(Color.GREEN);
                                            justPicked[0] = finalI;
                                        } catch (ArrayIndexOutOfBoundsException e) {
//                                            Snackbar.make(this.ll, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            showException(e, Picker.this);
                                        }
                                    }
                                });
                                try {
                                    resultString = listFiles[justPicked[0]].getCanonicalPath();
                                } catch (IOException | ArrayIndexOutOfBoundsException e) {
                                    showException(e, Picker.this);
                                }
                            } else {
                                runOnUiThread(() -> {
                                    this.currentPath = currentFile;
                                    File[] listFiles1 = currentFile.listFiles();
                                    fillViews(listFiles1, lp, unselectedColor, justPicked, ll);
                                });
                            }
                        });
                        runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
                case 2:
                    for (int i = 0; i < length; i++) {
                        extractM1(listFiles, lp, unselectedColor, textViews, i);
                        final int finalI = i;
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[finalI];
                            if (currentFile.isDirectory()) {
                                runOnUiThread(() -> {
                                    this.currentPath = currentFile;
                                    File[] listFiles1 = currentFile.listFiles();
                                    fillViews(listFiles1, lp, unselectedColor, justPicked, ll);
                                });
                            }
                        });
                        runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
            }
        }).start();
    }

    private void extractM1(File[] listFiles, LinearLayout.LayoutParams lp, int unselectedColor, TextView[] textViews,
                           int i) {
        textViews[i] = new TextView(this);
        textViews[i].setTextSize(25);
        textViews[i].setText(listFiles[i].isFile() ? listFiles[i].getName() : (listFiles[i].getName() + "/"));
        textViews[i].setLayoutParams(lp);
        switch (option) {
            case 1:
                runOnUiThread(() -> {
                    if (listFiles[i].isFile()) {
                        textViews[i].setBackgroundColor(white);
                    } else textViews[i].setBackgroundColor(unselectedColor);
                });
                break;
            case 2:
                runOnUiThread(() -> {
                    if (listFiles[i].isFile()) {
                        textViews[i].setBackgroundColor(unselectedColor);
                    } else {
                        textViews[i].setBackgroundColor(white);
                    }
                });
                break;
        }
    }

    private void previous() {
        try {
            File parentFile = this.currentPath.getParentFile();
            File[] listFiles = parentFile.listFiles();
            fillViews(listFiles, lp, grey, justPicked, ll);
            this.currentPath = parentFile;
        } catch (Exception e) {
            showException(e, this);
            finish();
        }
    }

    private void ioSetResult(String s) throws IOException {
        File file = new File(getFilesDir() + File.separator + "FilePickerResult");
        OutputStream os = new FileOutputStream(file, false);
        @SuppressWarnings("CharsetObjectCanBeUsed") byte[] bytes = s.getBytes("UTF-8");
        os.write(bytes);
        os.flush();
        os.close();
    }
}