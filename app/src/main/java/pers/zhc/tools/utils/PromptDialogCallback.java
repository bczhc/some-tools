package pers.zhc.tools.utils;

import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

public interface PromptDialogCallback {
    void confirm(EditText et, AlertDialog alertDialog);
}
