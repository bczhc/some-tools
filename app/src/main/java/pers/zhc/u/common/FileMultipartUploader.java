package pers.zhc.u.common;

import android.os.Build;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author bczhc
 */
public class FileMultipartUploader {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String upload(String urlString, File file) throws IOException {
        byte[] bytes = file.getName().getBytes(StandardCharsets.UTF_8);
        InputStream is = new FileInputStream(file);
        return MultipartUploader.formUpload(urlString, bytes, is);
    }
}