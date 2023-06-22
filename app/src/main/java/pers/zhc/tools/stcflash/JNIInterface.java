package pers.zhc.tools.stcflash;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

/**
 * The properties in the class are all the values defined in {@link UsbSerialPort}
 */
public class JNIInterface {
    private final SerialPool serialPool;
    private int baud = 0;
    private int parity = UsbSerialPort.PARITY_NONE;
    /**
     * 0 means infinite.
     */
    private int timeout = 0;
    private final UsbSerialPort port;

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

    public JNIInterface(UsbSerialPort port, SerialPool serialPool) {
        this.port = port;
        this.serialPool = serialPool;
        this.serialPool.run();
    }

    public void setSpeed(int baud) throws IOException {
        port.setParameters(baud, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, parity);
        this.baud = baud;
    }

    public byte[] read(int size) {
        return serialPool.read(size, timeout);
    }

    public int write(byte[] data) throws IOException {
        return port.write(data, 0);
    }

    public int getBaud() {
        return baud;
    }

    public void flush() throws IOException {
        try {
            port.purgeHwBuffers(true, true);
        } catch (UnsupportedOperationException ignored) {
        }
        serialPool.flush();
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout == -1 ? 0 : timeout;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setParity(byte p) throws IOException {
        for (int i = 0; i < nativeParityArray.length; i++) {
            byte b = nativeParityArray[i];
            if (b == p) {
                parity = portParityArray[i];
                break;
            }
        }
        port.setParameters(baud, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, parity);
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
