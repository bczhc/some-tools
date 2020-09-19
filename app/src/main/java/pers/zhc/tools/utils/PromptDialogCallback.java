package pers.zhc.tools.utils;

import android.app.AlertDialog;
import android.widget.EditText;

public interface PromptDialogCallback {
    void confirm(EditText et, AlertDialog alertDialog);
}
