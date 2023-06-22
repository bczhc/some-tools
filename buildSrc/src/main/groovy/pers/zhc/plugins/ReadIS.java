package pers.zhc.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ReadIS {
    private final InputStream is;
    private String charsetName = null;
    private Charset charset = null;

    public ReadIS(InputStream is) {
        this.is = is;
    }

    public ReadIS(InputStream is, String charsetName) {
        this.is = is;
        this.charsetName = charsetName;
    }

    public ReadIS(InputStream is, Charset charset) {
        this.is = is;
        this.charset = charset;
    }

    public void read(ReadISDO target) throws IOException {
        InputStreamReader isr;
        if (charset != null) {
            isr = new InputStreamReader(this.is, charset);
        } else if (charsetName != null) {
            isr = new InputStreamReader(this.is, charsetName);
        } else isr = new InputStreamReader(this.is);
        BufferedReader br = new BufferedReader(isr);
        String s = br.readLine();
        while (s != null) {
            target.f(s);
            s = br.readLine();
        }
    }

    public static String readToString(InputStream is, Charset charsets) throws IOException {
        StringBuilder sb = new StringBuilder();
        new ReadIS(is, charsets).read(sb::append);
        return sb.toString();
    }
}
