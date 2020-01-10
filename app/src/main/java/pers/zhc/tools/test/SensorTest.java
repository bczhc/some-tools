package pers.zhc.tools.test;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

import java.util.Arrays;

public class SensorTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_test_activity);
        TextView tv = findViewById(R.id.tv);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                System.out.println("event.values = " + Arrays.toString(event.values));
                tv.setText(Arrays.toString(event.values));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }
}
