package pers.zhc.tools.stcflash;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

/**
 * The properties in the class are all the values defined in {@link UsbSerialPort}
 */
public class JNIInterface {
    private int baud = 0;
    private int parity = UsbSerialPort.PARITY_NONE;
    /**
     * 0 means infinite.
     */
    private int timeout = 0;
    private UsbSerialPort port;

    private static final byte NATIVE_PARITY_NONE = 'N';
    private static final byte NATIVE_PARITY_EVEN = 'E';
    private static final byte NATIVE_PARITY_ODD = 'O';
    private static final byte NATIVE_PARITY_MARK = 'M';
    private static final byte NATIVE_PARITY_SPACE = 'S';

    private final byte[] nativeParityArray = {
            NATIVE_PARITY_NONE,
            NATIVE_PARITY_EVEN,
            NATIVE_PARITY_ODD,
            NATIVE_PARITY_MARK,
            NATIVE_PARITY_SPACE
    };

    /**
     * Corresponding to {@link JNIInterface#nativeParityArray}
     */
    private final byte[] portParityArray = {
            UsbSerialPort.PARITY_NONE,
            UsbSerialPort.PARITY_EVEN,
            UsbSerialPort.PARITY_ODD,
            UsbSerialPort.PARITY_MARK,
            UsbSerialPort.PARITY_SPACE
    };

    public JNIInterface(UsbSerialPort port) {
        this.port = port;
    }

    public void setSpeed(int baud) throws IOException {
        port.setParameters(baud, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, parity);
        this.baud = baud;
    }

    public byte[] read(int size) throws IOException {
        byte[] t = new byte[size];
        int readLen = port.read(t, timeout);
        byte[] r = new byte[readLen];
        System.arraycopy(t, 0, r, 0, readLen);
        return r;
    }

    public int write(byte[] data) throws IOException {
        return port.write(data, timeout);
    }

    public int getBaud() {
        return baud;
    }

    public void flush() {
        // no need
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout == -1 ? 0 : timeout;
    }

    public void setParity(byte p) {
        for (int i = 0; i < nativeParityArray.length; i++) {
            byte b = nativeParityArray[i];
            if (b == p) {
                parity = portParityArray[i];
                break;
            }
        }
    }

    public byte getParity() throws Exception {
        for (int i = 0; i < portParityArray.length; i++) {
            if (parity == portParityArray[i]) {
                return nativeParityArray[i];
            }
        }
        throw new Exception("Not found.");
    }

    public void close() throws IOException {
        port.close();
    }
}
