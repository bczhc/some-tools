package pers.zhc.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.text.SimpleDateFormat

/**
 * @author bczhc
 */
class MyPlugin1 implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println("hello, world")
    }

    static ran_sc(int min, int max) {
        double ran_sc_db = Math.round(Math.random() * (max - min)) + min
        return (int) ran_sc_db
    }

    static unicodeHexToString(int[] code) {
        StringBuilder sb = StringBuilder.newInstance()
        for (c in code) {
            if (c > 0xFFFF && c < 0x110000) {
                sb.append((Math.floor((c - 0x10000) / 0x400) + 0xD800) as char)
                        .append(((c - 0x10000) % 0x400 + 0xDC00) as char)
            } else
                sb.append(c as char)
        }
        return sb.toString()
    }

    static gVersion() {
        def date = Date.newInstance()
        def dateString = new SimpleDateFormat("yyyyMMdd").format(date)
        def time = new SimpleDateFormat("HHmmss").format(date)
        def which = 0
        def verString = "7.0.0.0"
        def emoji = ranEmoji()
        return [
                Integer.parseInt(dateString + which),
                "${verString}_${dateString}${time}_$emoji"
        ]
    }

    static ranEmoji() {
        def emojiRange = [[0x1F300, 0x1F5FF],
                          [0x1F900, 0x1F9FF],
                          [0x1F600, 0x1F64F],
                          [0x1F680, 0x1F6FF]]
        def hexArrLen = 3
        def hex = []
        for (int i = 0; i < hexArrLen; i++) {
            def index = ran_sc(0, emojiRange.size() - 1)
            hex[i] = ran_sc(emojiRange[index][0], emojiRange[index][1])
        }
        return unicodeHexToString(hex as int[])
    }

    static getNdkVersion() {
        def versionString = null;
        def ndkHome = System.getenv("ANDROID_NDK_HOME")
        if (ndkHome == null) throw new Exception("null environment variable \"ANDROID_NDK_HOME\"")
        def sourcePropertiesFile = new File(ndkHome + File.separator + "source.properties")
        def is = new FileInputStream(sourcePropertiesFile)
        def isr = new InputStreamReader(is)
        def br = new BufferedReader(isr)
        def lines = br.readLines()
        for (String line : lines) {
            if (line.matches(".*Pkg.Revision.*")) {
                versionString =  line.split(" = ")[1]
            }
        }
        br.close()
        is.close()
        println(versionString)
        return versionString
    }
}
