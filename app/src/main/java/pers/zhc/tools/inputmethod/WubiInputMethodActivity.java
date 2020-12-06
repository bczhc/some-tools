package pers.zhc.tools.inputmethod;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.Download;
import pers.zhc.tools.utils.ToastUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class WubiInputMethodActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialog checkDownloadDialog = new Dialog(this);
        checkDownloadDialog.setCancelable(false);
        checkDownloadDialog.setCanceledOnTouchOutside(false);
        TextView view = new TextView(this);
        view.setText(R.string.checking_local_needed_files);
        checkDownloadDialog.setContentView(view);
        checkDownloadDialog.show();

        try {
            URL wubiDatabaseURL = new URL(Common.getGithubRawFileURLString("bczhc", "master", "wubi_code.db"));
            URL md5TextFileURL = new URL(Common.getGithubRawFileURLString("bczhc", "master", "wubi_code.db.md5"));
            File localWubiDatabaseFile = Common.getInternalDatabaseDir(this, "wubi_code.db");
            Download.checkMD5(md5TextFileURL, localWubiDatabaseFile, result -> {
                checkDownloadDialog.dismiss();
                if (result) ready();
                else runOnUiThread(() -> Download.startDownloadWithDialog(this, wubiDatabaseURL, localWubiDatabaseFile, this::ready));
            });
        } catch (IOException e) {
            Common.showException(e, this);
        }
    }

    private void ready() {
        ToastUtils.show(this, "ok");
    }
}
