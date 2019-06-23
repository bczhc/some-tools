package com.zhc.qmcflac;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class P0 extends AppCompatActivity {
    private File pickD;
    private TextView tv;
    private File lD;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            Toast.makeText(this, Arrays.toString(grantResults), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
        setContentView(R.layout.p2);
        Button btn = null;
        Spinner spinner = null;
        Button btn2 = findViewById(R.id.lF);
        try {
            tv = findViewById(R.id.pathTV);
            File p = Environment.getExternalStorageDirectory();
//            File p = new File("/storage");
            tv.setText(String.format(getResources().getString(R.string.tv), p.toString()));
            spinner = findViewById(R.id.select);
            btn = findViewById(R.id.cdB);
            pickD = p;
            setSpinner(spinner, pickD);
            Spinner finalSpinner = spinner;
            btn.setOnClickListener(v -> {
                setSpinner(finalSpinner, pickD);
                tv.setText(String.format(getResources().getString(R.string.tv), pickD.toString()));
                lD = pickD;
                Intent intent = new Intent();
                intent.putExtra("f", tv.getText());
                setResult(2, intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
        if (btn != null) {
            Spinner finalSpinner1 = spinner;
            btn2.setOnClickListener(v -> {
                try {
                    lD = lD == null ? pickD : lD;
                    lD = lD.getParentFile();
                    setSpinner(finalSpinner1, lD);
                    tv.setText(String.format(getResources().getString(R.string.tv), lD.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setSpinner(Spinner spinner, File p) {
        try {
            List<String> data = new ArrayList<>();
            List<String> data_P = new ArrayList<>();
            File[] files = p.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    data.add(file.getName());
                    data_P.add(file.toString());
                }
            }
            SpinnerAdapter sData = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, data);
            runOnUiThread(() -> {
                spinner.setAdapter(sData);
                /*spinner2.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item
                        , data_F));*/
            });
            System.out.println("arr1 " + Arrays.toString(data.toArray(new String[0])));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    pickD = new File(data_P.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            /*spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //                runOnUiThread(() -> Toast.makeText(P0.this, pick_F.toString(), Toast.LENGTH_SHORT).show());
                    
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });*/
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        String intentS = tv.getText().toString();
        intent.putExtra("f", intentS);
        if (new File(intentS).exists()) {
            setResult(1, intent);
            super.onBackPressed();
        } else {
            Toast.makeText(this, "文件（夹）不存在或无法读取", Toast.LENGTH_SHORT).show();
        }
    }
}