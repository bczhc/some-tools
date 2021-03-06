package pers.zhc.plugins

import org.jetbrains.annotations.Nullable

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author bczhc
 */
class BuildUtils {
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
        def verString = "1.0.0"
        def emoji = ranEmoji()
        return [
                Integer.parseInt(dateString + which),
                "${verString}-${dateString}${time}-$emoji"
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

    static class Tools {
        private def ndkDir, sdkDir

        Tools(ndkDir, sdkDir) {
            this.ndkDir = ndkDir
            this.sdkDir = sdkDir
        }

        private static class Version {
            private int major, minor, build
            private File dir

            Version(int major, int minor, int build, File dir) {
                this.major = major
                this.minor = minor
                this.build = build
                this.dir = dir
            }

            @Override
            String toString() {
                return dir.name
            }
        }

        synchronized getCmakeVersionFromExecutable(String binDirPath) {
            def dir = new File(binDirPath)
            def executablePath = binDirPath + File.separatorChar + dir.list(new FilenameFilter() {
                @Override
                boolean accept(File d, String name) {
                    return name.matches("^cmake.*")
                }
            })[0]

            def runtime = Runtime.getRuntime()
            def is = runtime.exec([executablePath, "--version"] as String[]).inputStream
            def read = ReadIS.readToString(is, StandardCharsets.UTF_8)
            is.close()
            def matcher = Pattern.compile("(?<=cmake version )[0-9]+\\.[0-9]+\\.[0-9]").matcher(read)
            matcher.find()
            return matcher.group(0)
        }

        synchronized getCMakeVersion() {
            def cmakeDir = new File(this.sdkDir as String, "cmake")

            def binDirPath = (cmakeDir.path + File.separatorChar + "bin") as String
            if (new File(binDirPath).exists()) {
                return getCmakeVersionFromExecutable(binDirPath)
            }

            def versions = []
            def realCMakeRoot
            def subDirs = cmakeDir.listFiles()
            try {
                subDirs.each { def subDir ->
                    def matcher = Pattern.compile("[0-9]+").matcher(subDir.name)

                    if (!matcher.find()) throw new Exception("can't resolve version")
                    def major = matcher.group(0) as int

                    if (!matcher.find()) throw new Exception("can't resolve version")
                    def minor = matcher.group(0) as int

                    if (!matcher.find()) throw new Exception("can't resolve version")
                    def build = matcher.group(0) as int

                    def version = new Version(major, minor, build, subDir)
                    versions.add(version)
                }
                versions.sort { def o1, o2 ->
                    Version a = o1 as Version
                    Version b = o2 as Version
                    if (a.major == b.major) {
                        if (a.minor == b.minor) {
                            return a.build - b.build
                        }
                        return a.minor - b.minor
                    }
                    return a.major - b.major
                }
                realCMakeRoot = (versions[versions.size() - 1] as Version).dir
            } catch (def ignored) {
                realCMakeRoot = cmakeDir.listFiles()[0]
            }

            def sourcePropertiesFile = new File(realCMakeRoot, "source.properties")
            return getVersionStringFromPropertiesFile(sourcePropertiesFile)
        }

        synchronized getVersionStringFromPropertiesFile(File sourcePropertiesFile) {
            def versionString = null
            def is = new FileInputStream(sourcePropertiesFile)
            def isr = new InputStreamReader(is)
            def br = new BufferedReader(isr)
            def lines = br.readLines()
            for (String line : lines) {
                Matcher matcher = Pattern.compile("(?<=Pkg\\.Revision ?= ?).*").matcher(line)
                if (matcher.find()) {
                    versionString = matcher.group(0)
                }
            }
            br.close()
            is.close()
            return versionString
        }

        synchronized getNdkVersion() {
            def sourcePropertiesFile = new File(ndkDir as String, "source.properties")
            return getVersionStringFromPropertiesFile(sourcePropertiesFile)
        }
    }
}
