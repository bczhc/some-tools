package pers.zhc.u;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Compress extends FileU {
    public static void main(String[] args) throws IOException {
        File f = new File("C:/zhc/f/A/大壮-魔鬼中的天使.mp3");
        InputStream is = new FileInputStream(f);
        OutputStream os = compress(is);
//        System.out.println(Arrays.toString(((ByteArrayOutputStream) os).toByteArray()));
    }

    public static OutputStream compress(InputStream in) throws IOException {
        ByteArrayOutputStream r = new ByteArrayOutputStream();
        byte[] b = new byte[1];
        byte c, t = 0;
        while (true) {
            if (in.read(b) != -1) {
                c = b[0];
                if (c == t) {
                    System.out.println(c);
                }
                t = c;
            } else break;
        }
        return r;
    }
}