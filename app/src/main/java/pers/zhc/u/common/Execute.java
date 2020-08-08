package pers.zhc.u.common;


import pers.zhc.u.FileU;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@SuppressWarnings("Duplicates")
public class Execute {
    public static void exec(String command, @Documents.Nullable String charset) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process exec = runtime.exec(command);
        InputStream inputStream = exec.getInputStream();
        InputStream errorStream = exec.getErrorStream();
        InputStreamReader isr = new InputStreamReader(inputStream, charset == null ? "UTF-8" : charset);
        InputStreamReader isr2 = new InputStreamReader(errorStream, charset == null ? "UTF-8" : charset);
        FileU.readISR(isr);
        FileU.readISR(isr2);
        isr.close();
        isr2.close();
        inputStream.close();
        errorStream.close();
    }

    public static void exec(String[] command, @Documents.Nullable String charset) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process exec = runtime.exec(command);
        InputStream inputStream = exec.getInputStream();
        InputStream errorStream = exec.getErrorStream();
        InputStreamReader isr = new InputStreamReader(inputStream, charset == null ? "UTF-8" : charset);
        InputStreamReader isr2 = new InputStreamReader(errorStream, charset == null ? "UTF-8" : charset);
        FileU.readISR(isr);
        FileU.readISR(isr2);
        isr.close();
        isr2.close();
        inputStream.close();
        errorStream.close();
    }
}
