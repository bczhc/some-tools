package pers.zhc.tools.test;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.utils.CodepointIterator;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final CodepointIterator iter = new CodepointIterator("abcde完美的世界");

        for (Integer integer : iter) {
            Log.d(TAG, integer.toString());
        }
    }
}