package pers.zhc.tools.test;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

public class TTS extends BaseActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tts_test_activity);
        tts = new TextToSpeech(this, null);
        EditText et = findViewById(R.id.et);
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(v -> tts.speak(et.getText().toString(), TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis())));
    }
}
