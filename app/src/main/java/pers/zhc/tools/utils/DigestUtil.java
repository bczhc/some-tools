package pers.zhc.tools.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author bczhc
 */
public class DigestUtil {
    public static String getFileDigestString(File f, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);

        InputStream is = new FileInputStream(f);
        updateInputStream(md, is);
        is.close();

        byte[] sha1Bytes = md.digest();
        StringBuilder digestString = bytesToHexString(sha1Bytes);
        return digestString.toString();
    }

    /**
     * Append input stream to message digester.
     *
     * @param md message digest
     * @param is input stream
     * @throws IOException io exception
     */
    public static void updateInputStream(MessageDigest md, InputStream is) throws IOException {
        int readLen;
        byte[] buf = new byte[40960];

        while ((readLen = is.read(buf)) > 0) {
            md.update(buf, 0, readLen);
        }
    }

    @NotNull
    private static StringBuilder bytesToHexString(byte[] sha1Bytes) {
        StringBuilder digestString = new StringBuilder();
        for (byte sha1Byte : sha1Bytes) {
            String str = Integer.toHexString(sha1Byte < 0 ? (256 + sha1Byte) : sha1Byte);
            digestString.append(str.length() == 1 ? ('0' + str) : str);
        }
        return digestString;
    }
}
