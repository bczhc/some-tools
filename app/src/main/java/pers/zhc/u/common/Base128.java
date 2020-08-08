package pers.zhc.u.common;


import pers.zhc.u.FileU;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base128 &#x57fa;&#x4e8e;Base64&#x7684;&#x53d8;&#x79cd;&#x7f16;&#x7801;
 * &#x7f16;&#x7801;&#x4ee5;&#x521d;&#x7ea7;&#x76f4;&#x63a5;&#x7684;&#x65b9;&#x5f0f;&#x4e8e;20190304&#x5b9e;&#x73b0;&#xff0c;&#x4f1a;&#x9020;&#x6210;&#x5185;&#x5b58;&#x6ea2;&#x51fa;
 * &#x7f16;&#x7801;&#x90e8;&#x5206;&#x5b8c;&#x6210;&#x3002;&#x66f4;&#x597d;&#x7684;&#x5b9e;&#x73b0;&#x5f62;&#x5f0f;&#xff0c;&#x5360;&#x7528;&#x5185;&#x5b58;&#x5c0f; on 20190307
 * &#x89e3;&#x7801;&#x90e8;&#x5206;&#x5b8c;&#x6210; on 20190309
 * &#x6d4b;&#x8bd5;&#x6682;&#x65f6;&#x65e0;bug&#xff0c;&#x53ef;&#x5bf9;&#x6587;&#x4ef6;&#x8fdb;&#x884c;&#x7f16;&#x7801;&#xff0c;&#x5982;&#x679c;&#x6587;&#x4ef6;&#x5927;&#x5c0f;&#xff08;&#x5b57;&#x8282;&#x6570;&#xff09;&#x4e3a;7&#x7684;&#x6574;&#x6570;&#x500d;&#xff0c;&#x90a3;&#x4e48;&#x6e90;&#x6587;&#x4ef6;&#x548c;&#x7f16;&#x7801;&#x518d;&#x89e3;&#x7801;&#x540e;&#x7684;&#x6587;&#x4ef6;&#x7684;&#x6821;&#x9a8c;&#x7801;&#x4e00;&#x6837;&#x3002;
 * &#x4f46;&#x4e0d;&#x5f71;&#x54cd;&#x6587;&#x4ef6;&#x7684;&#x6253;&#x5f00;
 * &#x5b9e;&#x73b0;&#x4e86;&#x6e90;&#x6587;&#x4ef6;&#x548c;&#x7f16;&#x7801;&#x518d;&#x89e3;&#x7801;&#x540e;&#x7684;&#x6587;&#x4ef6;&#x6821;&#x9a8c;&#x7801;&#x4e00;&#x81f4; on 20190309
 * 20190316&#x7b97;&#x4e86;&#x7b97;&#x4e86;&#x3002;&#x3002;&#x672c;&#x6765;&#x8fd9;&#x4e2a;&#x6548;&#x7387;&#x53ef;&#x4ee5;&#x518d;&#x63d0;&#x9ad8;&#xff0c;&#x4e0d;&#x8fc7;&#x90a3;&#x8fd0;&#x884c;&#x65f6;&#x4e5f;&#x4f1a;&#x589e;&#x52a0;&#x5185;&#x5b58;&#x4f7f;&#x7528;&#x3002;&#x3002;&#x800c;&#x4e14;&#x8fd9;&#x6837;&#x6539;&#x57fa;&#x672c;&#x4e0a;&#x5c31;&#x662f;&#x8981;&#x91cd;&#x5199;&#x4e86;&#xff0c;&#x61d2;&#x5f97;&#x5f04;&#x3002;&#x3002;
 * &#x52a0;&#x5165;&#x5b8c;&#x6210;&#x540e;&#x6821;&#x9a8c;&#x529f;&#x80fd; 20190316
 * &#x5c06;&#x6821;&#x9a8c;&#x529f;&#x80fd;&#x5c01;&#x88c5;&#x8fdb;&#x65b9;&#x6cd5;&#xff0c;&#x8c03;&#x7528;&#x548c;&#x7ef4;&#x62a4;&#x67e5;&#x9519;&#x65b9;&#x4fbf;&#x4e86;&#x3002; on 20190317
 *
 * @author zhc
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Base128 {
    public static Base128 o = new Base128();
    private static long FileSize = 0;
    private String string;

    public Base128(String s) {
        this.string = s;
    }

    private Base128() {
    }

    /**
     * [-encode] [-decode] [&#x9700;&#x8981;&#x7f16;&#x7801;&#x7684;&#x6587;&#x4ef6;&#x4f4d;&#x7f6e;] [&#x7f16;&#x7801;&#x540e;&#x751f;&#x6210;&#x7684;&#x6587;&#x4ef6;&#x4f4d;&#x7f6e;&#xff08;&#x4e0d;&#x5b58;&#x5728;&#x5219;&#x521b;&#x5efa;&#xff09;]
     *
     * @param args args
     * @throws IOException e
     */
    public static void main(String[] args) throws IOException {
        if (!(args.length == 3 || (args.length == 4 && args[3] != null && !args[3].equals("")))) {
            new Base128().tip();
        } else {
            String arg0 = args[0];
            String arg1 = args[1];
            String arg2 = args[2];
            String arg3;
            try {
                arg3 = args[3];
            } catch (Exception ignored) {
                arg3 = "n";
            }
            boolean ck = arg3.equals("Y") || arg3.equals("y");
            File f1 = new File(arg1), f2 = new File(arg2);
            switch (arg0) {
                case "-encode":
                    String enR = Arrays.toString(encode(f1, f2, ck));
                    System.out.println(enR.equals("[]") ? "" : enR);
                    break;
                case "-decode":
                    String deR = Arrays.toString(decode(f1, f2, ck));
                    System.out.println(deR.equals("[]") ? "" : deR);
                    break;
            }
        }
    }

    public static OutputStream encode(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Base128().encode_extracted(in, baos);
        return baos;
    }

    public static void encode(InputStream in, OutputStream to_out) throws IOException {
        new Base128().encode_extracted(in, to_out);
    }

    public static OutputStream decode(InputStream in) throws IOException {
        ByteArrayOutputStream r = new ByteArrayOutputStream();
        new Base128().decode_extracted(in, r);
        return r;
    }

    public static void decode(InputStream in, OutputStream to_out) throws IOException {
        new Base128().decode_extracted(in, to_out);
    }

    public static String[] encode(File file, File dest, boolean check) throws IOException {
        String[] r = new String[0];
        if (!dest.exists()) System.out.println(dest.createNewFile());
        FileOutputStream fos = new FileOutputStream(dest, false);
        InputStream is = new FileInputStream(file);
        System.out.println("\u6b63\u5728\u7f16\u7801...");
        encode(is, fos);
        if (check) {
            r = new Base128().check_extracted("De", file, dest);
        }
        return r;
    }

    public static String[] decode(File file, File dest, boolean check) throws IOException {
        String[] r = new String[0];
        if (!dest.exists()) System.out.println(file.createNewFile());
        InputStream is2 = new FileInputStream(file);
        OutputStream os2 = new FileOutputStream(dest, false);
        System.out.println("\u6b63\u5728\u89e3\u7801...");
        decode(is2, os2);
        if (check) {
            r = new Base128().check_extracted("En", file, dest);
        }
        return r;
    }

    public static void encode(File file, File dest) throws IOException {
        System.out.println(Arrays.toString(encode(file, dest, false)));
    }

    public static void decode(File file, File dest) throws IOException {
        System.out.println(Arrays.toString(decode(file, dest, false)));
    }

    public static byte[] encode_text(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encode(bais, baos);
        bais.close();
        baos.close();
        return baos.toByteArray();
    }

    public static byte[] encode_text(String string, String charset) throws IOException {
        byte[] b = string.getBytes(charset);
        return encode_text(b);
    }

    public static byte[] encode_text(String string, Charset charset) throws IOException {
        byte[] b = string.getBytes(charset);
        return encode_text(b);
    }

    public static byte[] encode_text(String string) throws IOException {
        byte[] b = string.getBytes();
        for (byte b1 : b) {
            if (b1 < 0) {
                System.err.println("\u5b57\u7b26\u4e32\u542b\u6709\u975eASCII\u5185\u5b57\u7b26\uff0c\u56e0\u6b64\u9700\u8981\u6307\u5b9a\u7f16\u7801\u3002");
                return new byte[0];
            }
        }
        return encode_text(b);
    }

    public static void encode2(InputStream in, OutputStream Dest_out) throws IOException {
        long fS = in.available(), a = fS / 1029;
        int b = (int) (fS % 1029);
        byte[] r = new byte[1029], R = new byte[1176];
        byte[] b1 = {0, 0, 0, 0, 0, 'z', 'h', 'c'};
        b1[0] = (byte) (fS % 7);
        Dest_out.write(b1);
        for (int i = 0; i < a; i++) {
            //noinspection ResultOfMethodCallIgnored
            in.read(r);

        }
    }

    private List<String> ck_extracted(File f1, File new_ckF) throws IOException {
        List<String> r = new ArrayList<>();
        String md5_1 = FileU.getMD5String(new_ckF);
        String md5_2 = FileU.getMD5String(f1);
        if (md5_1.equals(md5_2)) {
            System.out.print("\u6821\u9a8c\u901a\u8fc7\uff01\n" + "MD5: " + md5_1);
            r.add("true");
            r.add(md5_1);
        } else {
            System.out.println("\u6821\u9a8c\u672a\u901a\u8fc7\u3002\u3002\u3002\u3002\n"
                    + md5_1 + "\n" + md5_2);
            r.add("false");
            r.add(md5_1);
            r.add(md5_2);
        }
        System.out.println("\ndelete: " + new_ckF.delete());
        return r;
    }

    private void decode_extracted(InputStream in, OutputStream to_out) throws IOException {
        FileSize = in.available();
        byte[] b = new byte[8], b3;
        long j = 0;
        byte[] F_f = new byte[1];
        System.out.println(in.read(F_f));
        int f = F_f[0];
        System.out.println("skip: " + in.skip(7));
        while (true) {
            b3 = new byte[7];
            StringBuilder sb = new StringBuilder();
            if (in.read(b) != -1) {
                j += 8;
                for (byte b1 : b) {
                    sb.append(new Base128().NumStr_lenTo(Integer.toBinaryString(b1), 7));
                }
                String[] s = new Base128().String_56_DivideInto(sb.toString(), 8);
                for (int i = 0; i < s.length; i++) {
                    int b2 = Integer.parseInt(s[i], 2);
                    b3[i] = (byte) -(-b2 & 0xFF);
                }
                if (in.available() >= 8) {
                    to_out.write(b3);
                } else {
                    byte[] b4 = new byte[f];
                    System.arraycopy(b3, 0, b4, 0, b4.length);
                    to_out.write(b4.length == 0 ? b3 : b4);
//                    to_out.write(b3, 0, f);
                }
                if (j % 1048576 == 0) {
                    System.out.println("progress: " + ((double) j / (double) FileSize) * 100 + "%");
                }
            } else break;
        }
        to_out.flush();
        to_out.close();
        System.out.println("progress: 100%\n" +
                "\u89e3\u7801\u5b8c\u6210\u3002");
    }

    private void encode_extracted(InputStream in, OutputStream to_out) throws IOException {
        long i = 0;
        FileSize = in.available();
        byte[] b;
        int[] b_i = new int[7];
        byte[] b1 = {-1, 0, 0, 0, 0, 'z', 'h', 'c'};
        b1[0] = (byte) (FileSize % 7L);
        to_out.write(b1);
        while (true) {
            b = new byte[7];
            if (in.read(b) != -1) {
                i += 7L;
                if (i % 1048572 == 0) {
                    System.out.println("progress: " + ((double) i / (double) FileSize) * 100 + "%");
                }
                for (int j = 0; j < b.length; j++) {
                    b_i[j] = (int) b[j] & 0xFF;
                }
                to_out.write(new Base128()._7Return_8(b_i));
            } else break;
        }
        to_out.flush();
        to_out.close();
        System.out.println("progress: 100%\n" +
                "\u7f16\u7801\u6210\u529f\uff01");
    }

    private String NumStr_lenTo(String string, int to) {
        boolean j = false;
        StringBuilder sb = new StringBuilder();
        if (string.length() < to) {
            j = true;
            for (int i = 0; i < to - string.length(); i++) {
                sb.append(0);
            }
            sb.append(string);
        }
        return j ? sb.toString() : string;
    }

    public String NumStr_lenTo(int to) {
        boolean j = false;
        StringBuilder sb = new StringBuilder();
        if (this.string.length() < to) {
            j = true;
            for (int i = 0; i < to - this.string.length(); i++) {
                sb.append(0);
            }
            sb.append(this.string);
        }
        return j ? sb.toString() : this.string;
    }

    private byte[] _7Return_8(int[] ints) {
        byte[] r = new byte[8];
        assert ints.length != 7;
        StringBuilder sb = new StringBuilder();
        for (int i : ints) {
            sb.append(NumStr_lenTo(Integer.toBinaryString(i), 8));
        }
        String[] strings = String_56_DivideInto(sb.toString(), 7);
        for (int i = 0; i < strings.length; i++) {
            r[i] = (byte) Integer.parseInt(strings[i], 2);
        }
        return r;
    }

    private String[] String_56_DivideInto(String s, int per_char_split) {
        assert s.length() != 56;
        String[] r = new String[56 / per_char_split];
        for (int i = 0; i < (56 / per_char_split); i++) {
            r[i] = s.substring(per_char_split * i, per_char_split * (i + 1));
        }
        return r;
    }

    private String[] check_extracted(String aArg_EnOrDe, File file, File dest) throws IOException {
        String[] r;
        System.out.print("\u6b63\u5728\u6821\u9a8c...");
        File ckF = new File(dest.getPath());
        File new_ckF = new FileU().creatFile_SameFileName(ckF);
        if (aArg_EnOrDe.equals("En")) {
            encode(new FileInputStream(dest), new FileOutputStream(new_ckF));
        } else if (aArg_EnOrDe.equals("De")) {
            decode(new FileInputStream(dest), new FileOutputStream(new_ckF));
        }
        r = new Base128().ck_extracted(file, new_ckF).toArray(new String[0]);
        return r;
    }

    private void tip() {
        System.out.println("Base128\u7f16\u7801\n\u547d\u4ee4\u884c\u53c2\u6570\u683c\u5f0f\uff1a" +
                "Command [-encode] [-decode] [\u9700\u8981\u7f16\u7801\u7684\u6587\u4ef6\u4f4d\u7f6e] " +
                "[\u7f16\u7801\u540e\u751f\u6210\u7684\u6587\u4ef6\u4f4d\u7f6e\uff08\u4e0d\u5b58\u5728\u5219\u521b" +
                "\u5efa\uff09] [\u5b8c\u6210\u540e\u662f\u5426\u6821\u9a8c\uff08y:\u662f | n:\u5426\uff09\uff08\u9ed8\u8ba4\u4e3an\uff09]" +
                "\n\u53c2\u6570\u4e2d\u6709\u7a7a\u683c\u9700\u52a0\u53cc\u5f15\u53f7");
    }

    public byte[] e1(byte[] buf) {
        byte[] r = new byte[8];
        r[0] = (byte) ((buf[0] & 255) >> 1);
        r[1] = (byte) (((buf[0] & 1) << 6) | ((buf[1] & 255) >> 2));
        r[2] = (byte) (((buf[1] & 3) << 5) | ((buf[2] & 255) >> 3));
        r[3] = (byte) (((buf[2] & 7) << 4) | ((buf[3] & 255) >> 4));
        r[4] = (byte) (((buf[3] & 15) << 3) | ((buf[4] & 255) >> 5));
        r[5] = (byte) (((buf[4] & 31) << 2) | ((buf[5] & 255) >> 6));
        r[6] = (byte) (((buf[5] & 63) << 1) | ((buf[6] & 255) >> 7));
        r[7] = (byte) (buf[6] & 127);
        return r;
    }

    public byte[] d1(byte[] buf) {
        byte[] r = new byte[7];
        r[0] = (byte) (((buf[0] & 255) << 1) | ((buf[1] & 255) >> 6));
        r[1] = (byte) (((buf[1] & 255) << 2) | ((buf[2] & 255) >> 5));
        r[2] = (byte) (((buf[2] & 255) << 3) | ((buf[3] & 255) >> 4));
        r[3] = (byte) (((buf[3] & 255) << 4) | ((buf[4] & 255) >> 3));
        r[4] = (byte) (((buf[4] & 255) << 5) | ((buf[5] & 255) >> 2));
        r[5] = (byte) (((buf[5] & 255) << 6) | ((buf[6] & 255) >> 1));
        r[6] = (byte) (((buf[6] & 255) << 7) | (buf[7] & 255));
        return r;
    }

    int e_1029P(byte[] buf, int readSize) {
        int a = readSize / 7, b = readSize % 7, g = (b == 0) ? a : a + 1, rL = g * 8;
        for (int i = 0; i < g; ++i) {
            byte[] e_buf = new byte[7], R = new byte[1176];
            int h = 0;
            for (int j = 7 * i; j < 8 * i; j++) {
                e_buf[h] = buf[j];
                ++h;
            }
            R = e1(e_buf);
        }
        return rL;
    }
}