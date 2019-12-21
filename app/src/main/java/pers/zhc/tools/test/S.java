package pers.zhc.tools.test;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import pers.zhc.tools.utils.Common;

import java.io.IOException;

public class S extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource("/storage/64AC-1C0E/Android/data/com.youku.phone/files/youku/offlinedata/XNTUxMDQ1Nzk2/youku.m3u8");
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Common.showException(e, this);
        }
    }
}
