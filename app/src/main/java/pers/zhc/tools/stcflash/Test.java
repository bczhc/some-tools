package pers.zhc.tools.stcflash;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;

public class Test {
    static void f(UsbSerialPort port) throws IOException {
        JNIInterface jniInterface = new JNIInterface(port);
        jniInterface.setSpeed(115200);
        jniInterface.setParity((byte) 'N');
        jniInterface.setTimeout(100);
        jniInterface.write(new byte[]{0x7f, 0x7f});
        byte[] read = jniInterface.read(3);
        System.out.println("read = " + Arrays.toString(read));
    }
}
