package pers.zhc.tools.filepicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.Documents;

import java.io.File;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import static pers.zhc.tools.utils.Common.showException;

@SuppressLint("ViewConstructor")
public class FilePickerRL extends RelativeLayout {
    @SuppressWarnings("unused")
    public static int TYPE_PICK_FILE = 1;
    @SuppressWarnings("unused")
    public static int TYPE_PICK_FOLDER = 2;
    private final File initialPath;
    private final Runnable cancelAction;
    private final OnPickedResultActionInterface pickedResultAction;
    private final int[] justPicked = new int[]{-1};
    public String result;
    private String resultString = "";
    private TextView pathView;
    private File currentPath;
    private LinearLayout ll;
    private LinearLayout.LayoutParams lp;
    private int grey = Color.parseColor("#DCDCDC");
    private int white = Color.WHITE;
    private int type;
    private Activity ctx;
    private Collator stringComparator;
    private Comparator<File> comparator;

    public FilePickerRL(Context context, int type, @Documents.Nullable File initialPath, Runnable cancelAction, OnPickedResultActionInterface pickedResultAction) {
        super(context);
        this.ctx = (Activity) context;
        this.type = type;
        this.initialPath = initialPath;
        this.cancelAction = cancelAction;
        this.pickedResultAction = pickedResultAction;
        init();
    }

    private void init() {
        View view = View.inflate(ctx, R.layout.file_picker_rl_activity, null);
        this.addView(view);
        this.lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        this.currentPath = initialPath == null ? new File(Common.getExternalStoragePath(ctx)) : initialPath;
//        this.currentPath = new File("/storage/emulated/0");
        lp.setMargins(2, 10, 10, 0);
        Button cancel = findViewById(R.id.cancel);
        Button ok = findViewById(R.id.pick);
        cancel.setOnClickListener(v -> {
            result = null;
            cancelAction.run();
        });
        ok.setOnClickListener(v -> {
            switch (type) {
                case 1:
                    result = resultString;
                    break;
                case 2:
                    String dir;
                    dir = currentPath.getAbsolutePath();
                    result = dir;
                    break;
            }
            pickedResultAction.result(result);
        });
        this.pathView = findViewById(R.id.textView);
        this.pathView.setOnClickListener((v) -> {
            AlertDialog.Builder ad = new AlertDialog.Builder(ctx);
            EditText et = new EditText(ctx);
            String s = pathView.getText().toString();
            ctx.runOnUiThread(() -> et.setText(ctx.getString(R.string.tv, s.equals("/storage/emulated") ? s + "/0" : s)));
            et.setLayoutParams(lp);
            AlertDialog alertDialog = ad.setTitle(R.string.type_path)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        File f = new File(et.getText().toString());
                        if (f.isFile() && type == 1) {
                            resultString = f.getAbsolutePath();
                            ok.performClick();
                        } else {
                            File[] listFiles = f.listFiles();
                            if (listFiles != null) {
                                Arrays.sort(listFiles, comparator);
                            }
                            this.currentPath = f;
                            fillViews(listFiles, lp, grey, justPicked, ll);
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .setView(et).create();
            DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            DialogUtil.setADWithET_autoShowSoftKeyboard(et, alertDialog);
            alertDialog.show();
        });
        this.ll = findViewById(R.id.ll);
        new Thread(() -> {
            File[] listFiles = currentPath.listFiles();
            if (listFiles != null) {
                Arrays.sort(listFiles, comparator);
            }
            fillViews(listFiles, lp, grey, justPicked, ll);
        }).start();
        stringComparator = Collator.getInstance(Locale.CHINA);
        comparator = (o1, o2) -> stringComparator.compare(o1.getName(), o2.getName());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fillViews(File[] listFiles, LinearLayout.LayoutParams lp, int unselectedColor,
                           int[] justPicked, LinearLayout ll) {
        new Thread(() -> {
            justPicked[0] = -1;
            ctx.runOnUiThread(ll::removeAllViews);
            ctx.runOnUiThread(() -> pathView.setText(String.format("%s", currentPath.getAbsolutePath())));
            TextView[] textViews;
            int length = 0;
            try {
                length = listFiles.length;
            } catch (Exception e) {
                ctx.runOnUiThread(() -> ToastUtils.show(ctx, R.string.no_access));
                e.printStackTrace();
            }
            textViews = new TextView[length];
            switch (type) {
                case 1:
                    for (int i = 0; i < length; i++) {
                        final int finalI = i;
                        extractM1(listFiles, lp, unselectedColor, textViews, i);
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[finalI];
                            if (currentFile.isFile()) {
                                Drawable background = textViews[finalI].getBackground();
                                ColorDrawable colorDrawable = (ColorDrawable) background;
                                if (colorDrawable.getColor() == Color.GREEN) {
                                    textViews[finalI].setBackgroundColor(white);
                                } else {
                                    try {
                                        if (justPicked[0] != -1)
                                            textViews[justPicked[0]].setBackgroundColor(white);
                                        textViews[finalI].setBackgroundColor(Color.GREEN);
                                        justPicked[0] = finalI;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        showException(e, ctx);
                                    }
                                }
                                try {
                                    resultString = listFiles[justPicked[0]].getAbsolutePath();
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    showException(e, ctx);
                                }
                            } else {
                                this.currentPath = currentFile;
                                File[] listFiles1 = currentFile.listFiles();
                                if (listFiles1 != null) {
                                    Arrays.sort(listFiles1, comparator);
                                }
                                fillViews(listFiles1, lp, unselectedColor, justPicked, ll);
                            }
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
                case 2:
                    for (int i = 0; i < length; i++) {
                        extractM1(listFiles, lp, unselectedColor, textViews, i);
                        final int finalI = i;
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[finalI];
                            if (currentFile.isDirectory()) {
                                this.currentPath = currentFile;
                                File[] listFiles1 = currentFile.listFiles();
                                if (listFiles1 != null) {
                                    Arrays.sort(listFiles1, comparator);
                                }
                                fillViews(listFiles1, lp, unselectedColor, justPicked, ll);
                            }
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
            }
        }).start();
    }

    /*@Override
    public void onBackPressed() {
        if (currentPath.equals(new File("/"))) finish();
        else previous();
    }*/

    private void extractM1(File[] listFiles, LinearLayout.LayoutParams lp, int unselectedColor, TextView[] textViews,
                           int i) {
        textViews[i] = new TextView(ctx);
        textViews[i].setBackgroundResource(R.drawable.view_stroke);
        textViews[i].setTextSize(25);
        ctx.runOnUiThread(() -> textViews[i].setText(listFiles[i].isFile() ? listFiles[i].getName() : (listFiles[i].getName() + "/")));
        textViews[i].setLayoutParams(lp);
        switch (type) {
            case 1:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundColor(white);
                } else textViews[i].setBackgroundColor(unselectedColor);
                break;
            case 2:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundColor(unselectedColor);
                } else {
                    textViews[i].setBackgroundColor(white);
                }
                break;
        }
    }

    public void previous() {
        if (currentPath.equals(new File(File.separator))) {
            cancelAction.run();
            return;
        }
        try {
            File parentFile = this.currentPath.getParentFile();
            File[] listFiles = new File[0];
            if (parentFile != null) {
                listFiles = parentFile.listFiles();
                if (listFiles != null) {
                    Arrays.sort(listFiles, comparator);
                }
            }
            fillViews(listFiles, lp, grey, justPicked, ll);
            this.currentPath = parentFile;
        } catch (Exception e) {
            showException(e, ctx);
        }
    }

    public interface OnPickedResultActionInterface {
        void result(String s);
    }
}