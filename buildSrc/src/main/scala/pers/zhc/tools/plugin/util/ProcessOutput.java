package pers.zhc.tools.plugin.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * @author bczhc
 */
public class ProcessOutput {
    public static void output(@NotNull Process process) throws IOException {
        final InputStream stdout = process.getInputStream();
        final InputStream stderr = process.getErrorStream();

        final Thread t1 = new Thread(() -> {
            try {
                write(stdout, System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        final Thread t2 = new Thread(() -> {
            try {
                write(stderr, System.err);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stdout.close();
        stderr.close();
    }

    private static void write(@NotNull InputStream in, PrintStream out) throws IOException {
        byte[] c = new byte[1];
        while (in.read(c) != -1) {
            out.write(c);
        }
    }
}
