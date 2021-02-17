package pers.zhc.tools.test.viewtest;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.views.ColorShowRL;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ColorShowRL colorShowRL = new ColorShowRL(this);
        colorShowRL.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.setContentView(colorShowRL);
        colorShowRL.setColor(Color.RED,"#FFFF0000");
    }
}
