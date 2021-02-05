package pers.zhc.tools.test;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;

/**
 * @author bczhc
 */
public class DocumentProviderTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra("CONTENT_TYPE", "*/*");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCode.START_ACTIVITY_0) {

        }
    }
}
