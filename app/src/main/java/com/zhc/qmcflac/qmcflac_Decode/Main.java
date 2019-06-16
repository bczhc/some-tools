package com.zhc.qmcflac.qmcflac_Decode;

import android.support.v7.app.AppCompatActivity;
import com.zhc.qmcflac.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main extends AppCompatActivity {
    @SuppressWarnings("Duplicates")
    public void Do_Decode(File file, File DestFile) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[fis.available()];
        System.out.println("fis.read(buffer) = " + fis.read(buffer));
        Decode dc = new Decode();
        int length = buffer.length;
        System.out.println("buffer.length = " + length);
        for (int i = 0; i < length; ++i) {
            buffer[i] = (byte) (dc.NextMask() ^ buffer[i]);
            int p = length / 20;
            if (i % p == 0)
//                runOnUiThread(() -> );
                MainActivity.tv.setText(String.format("%s"
                        , ((double) i / (double) length * 100D) + "%"));
        }
        FileOutputStream fos = new FileOutputStream(DestFile);
        fos.write(buffer);
        fos.flush();
        fos.close();
        fis.close();
    }
}