package pers.zhc.tools.epicycles;

import android.os.Bundle;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;

public class EpicyclesTest extends BaseActivity {

    private EpicyclesView epicyclesView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        epicyclesView = new EpicyclesView(this, EpicyclesEdit.epicyclesSequence);
        setContentView(epicyclesView);
    }

    @Override
    protected void onDestroy() {
        epicyclesView.shutdownES();
        super.onDestroy();
    }
}
