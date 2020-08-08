package pers.zhc.u;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class Digest {
    public static byte[] getFileMd5(File file) {
        InputStream is = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) messageDigest.update(buffer);
            return messageDigest.digest();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String getFileMd5String(File file) {
        byte[] bytes = getFileMd5(file);
        if (bytes == null) return null;
        return md5ToString(bytes);
    }

    public static String md5ToString(byte[] md5Bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
