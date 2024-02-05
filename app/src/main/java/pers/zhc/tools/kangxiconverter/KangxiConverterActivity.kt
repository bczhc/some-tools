package pers.zhc.tools.kangxiconverter;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.views.ScrollEditText;

public class kangxiConverterActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kangxi_converter_activity);
        Button kangxiRadicals2NormalHansBtn = findViewById(R.id.kangxi_radicals_to_normal_hans);
        Button normalHans2KangxiRadicalsBtn = findViewById(R.id.normal_hans_to_kangxi_radicals);
        ScrollEditText inputEt = findViewById(R.id.input_et);
        ScrollEditText outputEt = findViewById(R.id.output_et);
        kangxiRadicals2NormalHansBtn.setOnClickListener(v -> {
            String input = inputEt.getText().toString();
            String output = KangxiConverter.KangXi2Normal(input);
            outputEt.setText(output);
            KangxiConverter.markKangxiRadicalsEditText(inputEt.getEditText());
            KangxiConverter.markNormalHansEditText(outputEt.getEditText());
        });
        normalHans2KangxiRadicalsBtn.setOnClickListener(v -> {
            String input = inputEt.getText().toString();
            String output = KangxiConverter.normal2KangXi(input);
            outputEt.setText(output);
            KangxiConverter.markNormalHansEditText(inputEt.getEditText());
            KangxiConverter.markKangxiRadicalsEditText(outputEt.getEditText());
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}
