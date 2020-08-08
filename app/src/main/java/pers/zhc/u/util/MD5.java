package pers.zhc.u.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    /**
     * 生成32位MD5摘要
     *
     * @param string str
     * @return r
     */
    public static String md5(String string) {
        if (string == null) {
            return null;
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        byte[] btInput = string.getBytes();
        try {
            /* 获得MD5摘要算法的 MessageDigest 对象 */
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            /* 使用指定的字节更新摘要 */
            mdInst.update(btInput);
            /* 获得密文 */
            byte[] md = mdInst.digest();
            /* 把密文转换成十六进制的字符串形式 */
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }
}
