package pers.zhc.tools.epicycles;

import android.os.Bundle;
import android.support.annotation.Nullable;
import pers.zhc.tools.BaseActivity;

public class EpicyclesTest extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EpicyclesView epicyclesView = new EpicyclesView(this, EpicyclesEdit.epicyclesSequence);
        setContentView(epicyclesView);
    }
}
