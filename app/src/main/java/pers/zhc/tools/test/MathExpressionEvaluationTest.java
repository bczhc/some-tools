package pers.zhc.tools.test;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.mariuszgromada.math.mxparser.Expression;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;

public class MathExpressionEvaluationTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.math_evaluation_test_activity);
        EditText et = findViewById(R.id.et);
        TextView tv = findViewById(R.id.tv);
        Expression expression = new Expression();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                expression.setExpressionString(FloatingDrawingBoardMainActivity.completeParentheses(s.toString()));
                tv.setText(String.valueOf(expression.calculate()));
            }
        });
    }
}
