package com.zhc.tools.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.zhc.tools.R;

public class Clip extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clip_activity);
        Button btn = findViewById(R.id.copy);
        EditText et = findViewById(R.id.copy_et);
        btn.setOnClickListener(v -> {
            String s = et.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                ClipData cd = ClipData.newHtmlText("", getString(R.string.html_content), s);
                cm.setPrimaryClip(cd);
                Toast.makeText(this, getString(R.string.copying_success), Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, getString(R.string.html_clipboard_unsupported), Toast.LENGTH_SHORT).show();
        });
    }
}
