package pers.zhc.tools.test.epicycles_test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import pers.zhc.tools.BaseActivity;

public class ComplexGraphDrawing extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComplexGraphDrawingView complexGraphDrawingView = new ComplexGraphDrawingView(this);
        setContentView(complexGraphDrawingView);
    }
}
