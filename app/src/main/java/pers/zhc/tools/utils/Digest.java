package pers.zhc.tools.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author bczhc
 */
public class Digest {
    public static String getFileSHA1String(File f) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");

        InputStream is = new FileInputStream(f);
        int readLen;
        byte[] buf = new byte[40960];

        while ((readLen = is.read(buf)) >0) {
            md.update(buf, 0, readLen);
        }

        is.close();

        byte[] sha1Bytes = md.digest();
        StringBuilder digestString = new StringBuilder();
        for (byte sha1Byte : sha1Bytes) {
            String str = Integer.toHexString(sha1Byte < 0 ? (256 + sha1Byte) : sha1Byte);
            digestString.append(str.length() == 1 ? ('0' + str) : str);
        }
        return digestString.toString();
    }
}
