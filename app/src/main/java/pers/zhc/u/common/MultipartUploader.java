package pers.zhc.u.common;

import pers.zhc.u.FileU;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultipartUploader {
    /**
     * @param urlStr   url
     * @param headByte head information bytes
     * @param is       inputStream
     * @return 返回response数据
     */
    public static String formUpload(String urlStr, @Documents.Nullable byte[] headByte, InputStream is) throws IOException {
        String res;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            OutputStream out = setConnection(conn, headByte);
            // file
            FileU.StreamWrite(is, out);
            res = getString(conn, out);
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            throw e;
        } finally {
            is.close();
        }
        return res;
    }

    public static String formUpload(String urlStr, @Documents.Nullable byte[] headByte, byte[] data) throws IOException {
        String res;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            OutputStream out = setConnection(conn, headByte);
            out.write(data);
            out.flush();
            res = getString(conn, out);
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            throw e;
        }
        return res;
    }

    private static String getString(HttpURLConnection conn, OutputStream out) throws IOException {
        String res;
        out.close();
        // 读取返回数据
        StringBuilder strBuf = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            strBuf.append(line).append("\n");
        }
        res = strBuf.toString();
        reader.close();
        return res;
    }

    private static OutputStream setConnection(HttpURLConnection conn, byte[] headByte) throws IOException {
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        OutputStream out = new DataOutputStream(conn.getOutputStream());
        if (headByte != null) {
            out.write(headByte);
            out.write(new byte[]{0});
            out.flush();
        }
        return out;
    }
}
