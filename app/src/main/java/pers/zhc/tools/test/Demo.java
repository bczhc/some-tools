package pers.zhc.tools.test;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.diary.DiaryBaseActivity;
import pers.zhc.tools.fdb.PanelRL;

/**
 * @author bczhc
 */
public class Demo extends DiaryBaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PanelRL panelRL = new PanelRL(this);
        setContentView(panelRL);
    }
}