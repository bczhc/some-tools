package com.zhc;

import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Class {
    // 计算字符串的MD5
    private static String conVertTextToMD5() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update("hello".getBytes());
            byte[] b = md.digest();

            int i;

            StringBuilder buf = new StringBuilder();
            for (byte b1 : b) {
                i = b1;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            // 32位加密
            return buf.toString();
            // 16位的加密
            // return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

//计算文件的MD5，支持4G一下的文件（文件亲测，大文件未亲测）
    public static String conVertFileToMD5(String inputFilePath) {
        int bufferSize = 256 * 1024;

        FileInputStream fileInputStream = null;

        DigestInputStream digestInputStream = null;

        try {

            // 拿到一个MD5转换器（同样，这里可以换成SHA1）

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // 使用DigestInputStream

            fileInputStream = new FileInputStream(inputFilePath);

            digestInputStream = new DigestInputStream(fileInputStream,
                    messageDigest);

            // read的过程中进行MD5处理，直到读完文件

            byte[] buffer = new byte[bufferSize];

            //noinspection StatementWithEmptyBody
            while (digestInputStream.read(buffer) > 0);

            // 获取最终的MessageDigest

            messageDigest = digestInputStream.getMessageDigest();

            // 拿到结果，也是字节数组，包含16个元素

            byte[] resultByteArray = messageDigest.digest();

            // 同样，把字节数组转换成字符串

            return byteArrayToHex(resultByteArray);

        } catch (Exception e) {

            return null;

        } finally {

            try {

                assert digestInputStream != null;
                digestInputStream.close();

            } catch (Exception ignored) {

            }

            try {

                fileInputStream.close();

            } catch (Exception ignored) {

            }

        }
    }

    private static String byteArrayToHex(byte[] byteArray) {

        // 首先初始化一个字符数组，用来存放每个16进制字符

        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };

        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））

        char[] resultCharArray = new char[byteArray.length * 2];

        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去

        int index = 0;

        for (byte b : byteArray) {

            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];

            resultCharArray[index++] = hexDigits[b & 0xf];

        }

        // 字符数组组合成字符串返回

        return new String(resultCharArray);
    }

    public static void main(String[] args) {
        // 测试
        System.out.println(MD5Class.conVertTextToMD5());
        System.out
                .println(conVertFileToMD5("C:\\Users\\administrator1\\Downloads\\StarUML-v2.8.0.msi"));
    }
}