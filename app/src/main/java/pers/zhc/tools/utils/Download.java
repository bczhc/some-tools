package pers.zhc.tools.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

public class Download {
    public static void download(@NotNull URL url, OutputStream os, @Nullable ProgressCallback progressCallback, @Nullable Runnable doneAction) throws IOException {
        URLConnection connection = url.openConnection();
        byte[] buf = new byte[4096];

        long contentLength;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentLength = connection.getContentLengthLong();
        } else contentLength = connection.getContentLength();

        long totalRead = 0;
        InputStream is = connection.getInputStream();
        int readLen;
        while ((readLen = is.read(buf)) > 0) {
            os.write(buf, 0, readLen);
            totalRead += readLen;
            if (progressCallback != null) {
                progressCallback.call(((float) totalRead) / ((float) contentLength));
            }
        }
        is.close();
        if (doneAction != null) {
            doneAction.run();
        }
    }

    public static void checkMD5(URL md5TextFileUrl, @NotNull File localFile, ResultCallback<Boolean> resultCallback) {
        if (!localFile.exists()) resultCallback.result(false);
        AtomicReference<String> md5 = new AtomicReference<>("");
        new Thread(() -> {
            try {
                String localFileMD5 = DigestUtil.getFileDigestString(localFile, "MD5");
                InputStream is = md5TextFileUrl.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                md5.set(br.readLine());
                isr.close();
                is.close();
                br.close();
                resultCallback.result(md5.get().equalsIgnoreCase(localFileMD5));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static void startDownloadWithDialog(Context ctx, URL url, File localFile, @Nullable Runnable doneAction) {
        Dialog downloadDialog = new Dialog(ctx);

        DialogUtil.setDialogAttr(downloadDialog, false
                , ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                , false);
        RelativeLayout rl = View.inflate(ctx, R.layout.progress_bar, null)
                .findViewById(R.id.rl);
        rl.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        TextView progressTextView = rl.findViewById(R.id.progress_tv);
        TextView barTextView = rl.findViewById(R.id.progress_bar_title);
        barTextView.setText(R.string.downloading);
        ProgressBar pb = rl.findViewById(R.id.progress_bar);
        progressTextView.setText(R.string.please_wait);
        downloadDialog.setContentView(rl);
        downloadDialog.setCanceledOnTouchOutside(false);
        downloadDialog.setCancelable(true);
        downloadDialog.show();

        try {
            FileOutputStream os = new FileOutputStream(localFile, false);

            final AsyncTryDo tryDo = new AsyncTryDo();

            new Thread(() -> {
                try {
                    download(url, os, progress -> {

                        tryDo.tryDo((self, notifier) -> {
                            Common.runOnUiThread(ctx, () -> {
                                pb.setProgress(((int) (progress * 100F)));
                                progressTextView.setText(ctx.getString(R.string.progress_tv, progress * 100F));
                                notifier.finish();
                            });
                        });

                    }, () -> {
                        downloadDialog.dismiss();
                        if (doneAction != null) {
                            doneAction.run();
                        }
                    });
                } catch (IOException e) {
                    Common.showException(e, (Activity) ctx);
                    downloadDialog.dismiss();
                }
            }).start();
        } catch (IOException e) {
            Common.showException(e, (Activity) ctx);
            downloadDialog.dismiss();
        }
    }

    interface ProgressCallback {
        void call(float f);
    }
}
