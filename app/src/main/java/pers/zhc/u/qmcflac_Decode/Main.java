package pers.zhc.u.qmcflac_Decode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("WeakerAccess")
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Command [Source file] [Dest file]\n");
            return;
        }
        new Main().Do_Decode(args[0], args[1]);
    }

    public void Do_Decode(String file, String DestFile) throws IOException {
        Do_Decode(new File(file), new File(DestFile));
    }

    public void Do_Decode(File file, File DestFile) throws IOException {
        /*FileInputStream fis = new FileInputStream(file); //打开文件
        byte[] buffer = new byte[fis.available()]; //读取整个文件 二进制进缓存byte数组
        System.out.println("fis.read(buffer) = " + fis.read(buffer));
        Decode dc = new Decode();
        for (int i = 0; i < buffer.length; ++i) { //遍历整个缓存数组
            buffer[i] = (byte) (dc.NextMask() ^ buffer[i]); //替换，第i个就等于……
        }
        FileOutputStream fos = new FileOutputStream(DestFile); //打开输出文件流
        fos.write(buffer); //写入
        fos.flush(); //压入
        fos.close(); //关闭
        fis.close(); //关闭*/
        Decode dc = new Decode();
        InputStream is = new FileInputStream(file);
        OutputStream os = new FileOutputStream(DestFile);
        byte[] bytes = new byte[2048], r = new byte[2048];
        int readLen;
        while (true) {
            if ((readLen = is.read(bytes)) != -1) {
                for (int i = 0; i < readLen; i++) {
                    r[i] = (byte) (bytes[i] ^ dc.NextMask());
                }
                os.write(r, 0, readLen);
                os.flush();
            } else break;
        }
        os.close();
        is.close();
    }
}