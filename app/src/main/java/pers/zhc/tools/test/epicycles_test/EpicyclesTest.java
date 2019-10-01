package pers.zhc.tools.test.epicycles_test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import pers.zhc.tools.R;

public class EpicyclesTest extends AppCompatActivity {
    private EpicyclesView epicyclesView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.epicycles_test_activity);
        RelativeLayout rl = findViewById(R.id.rl);
        SeekBar sb = findViewById(R.id.sb);
        /*epicyclesSequence.put(3, new ComplexValue(30D, 30D));
        epicyclesSequence.put(3, new ComplexValue(30D, 30D));
        epicyclesSequence.put(3, new ComplexValue(30D, 30D));
        epicyclesSequence.put(4, new ComplexValue(40D, -30D));
        epicyclesSequence.put(4, new ComplexValue(40D, -30D));
        epicyclesSequence.put(-4, new ComplexValue(50D, 50D));
        epicyclesSequence.put(-4, new ComplexValue(50D, 50D));
        epicyclesSequence.put(2, new ComplexValue(-20D, 60D));
        epicyclesSequence.put(2, new ComplexValue(-20D, 60D));
        epicyclesSequence.put(-2, new ComplexValue(100D, 100D));
        epicyclesSequence.put(-2, new ComplexValue(100D, 100D));
        epicyclesSequence.put(-3, new ComplexValue(-100D, -10D));
        epicyclesSequence.put(-3, new ComplexValue(-100D, -10D));
        epicyclesSequence.put(6, new ComplexValue(50D, -40D));
        epicyclesSequence.put(6, new ComplexValue(50D, -40D));
        epicyclesSequence.put(-4, new ComplexValue(100D, 20D));
        epicyclesSequence.put(-4, new ComplexValue(100D, 20D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));*/
//        epicyclesSequence.put(4, 1, 1);
//        epicyclesSequence.put(1, new ComplexValue(100, 100));


        /*epicyclesSequence.put(0, 10.72, 16.52);
        epicyclesSequence.put(1, -12.64, 20.90);
        epicyclesSequence.put(-1, -135.66, -45.57);
        epicyclesSequence.put(2, -44.85, -23.71);
        epicyclesSequence.put(-2, 66.75, -53.07);*/
        epicyclesView = new EpicyclesView(this, EpicyclesEdit.epicyclesSequence);
        sb.setMax(100);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                epicyclesView.scale(((double) progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rl.addView(epicyclesView);
    }

    @Override
    protected void onStop() {
        epicyclesView.shutdownES();
        super.onStop();
    }
}
