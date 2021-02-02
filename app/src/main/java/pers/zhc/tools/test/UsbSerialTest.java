package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.Common;

public class UsbSerialTest extends BaseActivity {
    private UsbSerialPort port;
    private UsbDeviceConnection usbDeviceConnection;
    private static int fd;
    private String name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usb_serial_test_activity);

        Button sendBtn = findViewById(R.id.send_btn);
        Button receiveBtn = findViewById(R.id.receive_btn);
        TextView tv = findViewById(R.id.tv);
        EditText et = findViewById(R.id.et);

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        UsbDevice first = deviceList.values().iterator().next();
        name = first.getDeviceName();
        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("usb")) {
                    List<UsbSerialDriver> allDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                    UsbSerialDriver usbSerialDriver = allDrivers.get(0);
                    usbDeviceConnection = usbManager.openDevice(usbSerialDriver.getDevice());
                    fd = usbDeviceConnection.getFileDescriptor();
                    port = usbSerialDriver.getPorts().get(0);
                    try {
                        port.open(usbDeviceConnection);
                    } catch (IOException e) {
                        Common.showException(e, UsbSerialTest.this);
                    }
                }
            }
        };
        registerReceiver(usbReceiver, new IntentFilter("usb"));
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, RequestCode.REQUEST_USB_PERMISSION, new Intent("usb"), 0);
        usbManager.requestPermission(first, permissionIntent);

        sendBtn.setOnClickListener(v -> {
            /*try {
                String s = et.getText().toString();
                port.write(s.getBytes(StandardCharsets.UTF_8), 0);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            JNI.JniTest.call(usbDeviceConnection.getFileDescriptor());
            System.out.println("name = " + name);
//            JNI.JniTest.call(name);
        });

        receiveBtn.setOnClickListener(v -> new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                byte[] r = timeoutRead(port, 10, 2000);
                long end = System.currentTimeMillis();
                @SuppressLint("DefaultLocale") String msg = String.format("data: %s\ntext: %s\ntime: %d\ntimestamp: %d",
                        Arrays.toString(r),
                        new String(r, StandardCharsets.UTF_8),
                        end - start,
                        System.currentTimeMillis()
                );
                runOnUiThread(() -> tv.setText(msg));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start());
    }

    @Override
    public void finish() {
        try {
            port.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.finish();
    }

    private static final byte[] buf = new byte[64*4];

    public static byte[] timeoutRead(UsbSerialPort port, int size, int timeout) throws IOException {
        try {
            if (fd == -1) {
                throw new Exception("fd is -1");
            }
            JNI.JniTest.call(fd);
        } catch(Exception e) {
            e.printStackTrace();
        }
        int haveRead = 0;
        long start = System.currentTimeMillis();
        ArrayList<Byte> b = new ArrayList<>(size);
        for (; ; ) {
            long goneTime = System.currentTimeMillis() - start;
            if (goneTime >= timeout) {
                // timeout occurred
                break;
            }
            int readLen = port.read(buf, (int) (timeout - goneTime));

            if (readLen == 0 && goneTime >= timeout) {
                // timeout occurred
                break;
            }
            boolean broken = false;
            for (int i = 0; i < readLen; i++) {
                b.add(buf[i]);
                if (b.size() == size) {
                    // enough data
                    broken = true;
                    break;
                }
            }
            if (broken) break;
            haveRead += readLen;
            if (haveRead == size) {
                // enough data
                break;
            }
        }
        byte[] r = new byte[b.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = b.get(i);
        }
        return r;
    }
}