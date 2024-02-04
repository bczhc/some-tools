package pers.zhc.tools.test;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.KangxiConverter;
import pers.zhc.tools.R;
import pers.zhc.tools.views.ScrollEditText;

public class TTS extends BaseActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tts_test_activity);
        tts = new TextToSpeech(this, null);
        ScrollEditText et = findViewById(R.id.et);
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(v -> {
            tts.speak(KangxiConverter.KangXi2Normal(et.getText().toString()), TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis()));
            KangxiConverter.markKangxiRadicalsEditText(et.getEditText());
        });
    }
}
