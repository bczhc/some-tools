package pers.zhc.tools.filepicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.Documents;

import java.io.File;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static pers.zhc.tools.utils.Common.showException;

@SuppressLint("ViewConstructor")
public class FilePickerRL extends RelativeLayout {
    public static final int TYPE_PICK_FILE = 1;
    public static final int TYPE_PICK_FOLDER = 2;
    private final File initialPath;
    private final Runnable cancelAction;
    private final OnPickedResultActionInterface pickedResultAction;
    private final int[] justPicked = new int[]{-1};
    private final @DrawableRes
    int cannotPick = R.drawable.file_picker_view_cannot_pick;
    private final @DrawableRes
    int canPickUnchecked = R.drawable.file_picker_view_can_pick_unchecked;
    private final @DrawableRes
    int viewChecked = R.drawable.file_picker_view_checked;
    private final int type;
    private final Activity ctx;
    public String result;
    private String resultString = "";
    private TextView pathView;
    private File currentPath;
    private LinearLayout ll;
    private LinearLayout.LayoutParams lp;
    private EditText headET;
    private @DrawableRes int unselectedDrawable;

    public FilePickerRL(Context context, int type, @Documents.Nullable File initialPath
            , Runnable cancelAction, OnPickedResultActionInterface pickedResultAction
            , @Nullable String initFileName) {
        super(context);
//        this.currentFiles = new LinkedList<>();
        this.ctx = (Activity) context;
        this.type = type;
        this.initialPath = initialPath;
        this.cancelAction = cancelAction;
        this.pickedResultAction = pickedResultAction;
        initFileName = initFileName == null ? "" : initFileName;
        init(initFileName);
    }

    private void init(String initFileName) {
        this.unselectedDrawable = this.type == TYPE_PICK_FILE ? cannotPick : canPickUnchecked;
        View view = View.inflate(ctx, R.layout.file_picker_rl_activity, null);
        this.addView(view);
        this.lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        this.currentPath = initialPath == null ? new File(Common.getExternalStoragePath(ctx)) : initialPath;
        headET = findViewById(R.id.file_name_et);
        headET.setText(initFileName);
        headET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                File[] fileList = getFileList(FilePickerRL.this.currentPath);
                fillViews(fileList);
            }
        });
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
            ctx.runOnUiThread(() -> et.setText(ctx.getString(R.string.tv, (s.equals("/storage/emulated") ? s + "/0" : s) + '/')));
            et.setLayoutParams(lp);
            AlertDialog alertDialog = ad.setTitle(R.string.type_path)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String etText = et.getText().toString();
                        if (etText.charAt(0) != '/') etText = '/' + etText;
                        File f = new File(etText);
                        if (f.isFile() && type == 1) {
                            resultString = f.getAbsolutePath();
                            ok.performClick();
                        } else {
                            File[] listFiles = getFileList(f);
                            this.currentPath = f;
                            fillViews(listFiles);
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .setView(et).create();
            DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null);
            DialogUtil.setADWithET_autoShowSoftKeyboard(et, alertDialog);
            alertDialog.show();
        });
        this.ll = findViewById(R.id.ll);
        new Thread(() -> {
            File[] listFiles = getFileList(currentPath);
            fillViews(listFiles);
        }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void fillViews(File[] listFilesP) {
        final File[][] listFiles = {listFilesP};
        Thread thread = new Thread(() -> {
            listFiles[0] = filter(listFiles[0], headET.getText().toString());
            justPicked[0] = -1;
            ctx.runOnUiThread(ll::removeAllViews);
            ctx.runOnUiThread(() -> pathView.setText(String.format("%s", currentPath.getAbsolutePath())));
            TextViewWithExtra[] textViews;
            int length = 0;
            try {
                length = listFiles[0].length;
            } catch (Exception e) {
                ctx.runOnUiThread(() -> ToastUtils.show(ctx, R.string.no_access));
                e.printStackTrace();
            }
            textViews = new TextViewWithExtra[length];
            switch (type) {
                case TYPE_PICK_FILE:
                    for (int i = 0; i < length; i++) {
                        final int finalI = i;
                        extractM1(listFiles[0], lp, textViews, i);
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[0][finalI];
                            if (currentFile.isFile()) {
                                if (textViews[finalI].picked) {
                                    textViews[finalI].setBackgroundResource(canPickUnchecked);
                                } else {
                                    try {
                                        if (justPicked[0] != -1)
                                            textViews[justPicked[0]].setBackgroundResource(canPickUnchecked);
                                        textViews[finalI].setBackgroundResource(viewChecked);
                                        justPicked[0] = finalI;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        showException(e, ctx);
                                    }
                                }
                                try {
                                    resultString = listFiles[0][justPicked[0]].getAbsolutePath();
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    showException(e, ctx);
                                }
                            } else {
                                this.currentPath = currentFile;
                                File[] listFiles1 = getFileList(currentFile);
                                fillViews(listFiles1);
                            }
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
                case TYPE_PICK_FOLDER:
                    for (int i = 0; i < length; i++) {
                        extractM1(listFiles[0], lp, textViews, i);
                        final int finalI = i;
                        textViews[i].setOnClickListener(v -> {
                            File currentFile = listFiles[0][finalI];
                            if (currentFile.isDirectory()) {
                                this.currentPath = currentFile;
                                File[] listFiles1 = getFileList(currentFile);
                                fillViews(listFiles1);
                            }
                        });
                        ctx.runOnUiThread(() -> ll.addView(textViews[finalI]));
                    }
                    break;
            }
        });
        thread.start();
    }

    /*@Override
    public void onBackPressed() {
        if (currentPath.equals(new File("/"))) finish();
        else previous();
    }*/

    private void extractM1(File[] listFiles, LinearLayout.LayoutParams lp, TextViewWithExtra[] textViews,
                           int i) {
        textViews[i] = new TextViewWithExtra(ctx);
        textViews[i].setBackgroundResource(R.drawable.view_stroke);
        textViews[i].setTextSize(25);
        ctx.runOnUiThread(() -> textViews[i].setText(listFiles[i].isFile() ? listFiles[i].getName() : (listFiles[i].getName() + "/")));
        textViews[i].setLayoutParams(lp);
        switch (type) {
            case 1:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundResource(canPickUnchecked);
                } else textViews[i].setBackgroundResource(unselectedDrawable);
                break;
            case 2:
                if (listFiles[i].isFile()) {
                    textViews[i].setBackgroundResource(unselectedDrawable);
                } else {
                    textViews[i].setBackgroundResource(canPickUnchecked);
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
                listFiles = getFileList(parentFile);
            }
            fillViews(listFiles);
            this.currentPath = parentFile;
        } catch (Exception e) {
            showException(e, ctx);
        }
    }

    private File[] getFileList(File file) {
        File[] files = file.listFiles();
        List<File> fileList = new LinkedList<>();
        List<File> dirList = new LinkedList<>();
        Collator stringComparator = Collator.getInstance(Locale.CHINA);
        Comparator<File> comparator = (o1, o2) -> stringComparator.compare(o1.getName(), o2.getName());
        if (files == null) {
            files = new File[0];
        }
        Arrays.sort(files, comparator);
        for (File f : files) {
            if (f.isDirectory()) dirList.add(f);
            else fileList.add(f);
        }
        List<File> r = new LinkedList<>();
        r.addAll(dirList);
        r.addAll(fileList);
        return r.toArray(new File[0]);
    }

    private File[] filter(File[] files, String regex) {
        List<File> fileList = new LinkedList<>();
        for (File file : files) {
            if (regex.equals("")) {
                fileList.add(file);
            } else {
                boolean match = true;
                try {
                    match = file.getName().matches(regex);
                } catch (Exception ignored) {
                    ToastUtils.show(ctx, R.string.wrong_regex);
                }
                if (match) {
                    fileList.add(file);
                }
            }
        }
        return fileList.toArray(new File[0]);
    }

    public interface OnPickedResultActionInterface {
        void result(String s);
    }

    private static class TextViewWithExtra extends TextView {
        private boolean picked = false;

        @Override
        public void setBackgroundResource(int resId) {
            super.setBackgroundResource(resId);
            this.picked = (resId == R.drawable.file_picker_view_checked);
        }

        public TextViewWithExtra(Context context) {
            super(context);


        }
    }
}