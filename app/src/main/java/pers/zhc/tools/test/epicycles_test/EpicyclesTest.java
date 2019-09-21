package pers.zhc.tools.test.epicycles_test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

public class EpicyclesTest extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EpicyclesSequence epicyclesSequence = new EpicyclesSequence();
        epicyclesSequence.put(1, new ComplexValue(30D, 30D));
        epicyclesSequence.put(1, new ComplexValue(40D, -30D));
        epicyclesSequence.put(-1, new ComplexValue(50D, 50D));
        epicyclesSequence.put(2, new ComplexValue(-20D, 60D));
        epicyclesSequence.put(-2, new ComplexValue(100D, 100D));
        epicyclesSequence.put(-3, new ComplexValue(-100D, -10D));
        epicyclesSequence.put(3, new ComplexValue(50D, -40D));
        epicyclesSequence.put(-4, new ComplexValue(100D, 20D));
        epicyclesSequence.put(4, new ComplexValue(0D, -50D));
        EpicyclesView epicyclesView = new EpicyclesView(this, epicyclesSequence);
        setContentView(epicyclesView);
    }
}
