package pers.zhc.plugins

import org.apache.commons.io.FileUtils as ApacheFileUtils
import org.gradle.api.GradleException
import pers.zhc.util.Assertion

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
            } else sb.append(c as char)
        }
        return sb.toString()
    }

    static generateVersion() {
        // minutes since UNIX_EPOCH
        def versionCode = (System.currentTimeMillis() / 1_000_000) as int
        def versionName = ""

        def now = OffsetDateTime.now(ZoneOffset.UTC)
        versionName += now.format("yyyyMMddHHmmss")

        try {
            def gitVersion = Runtime.getRuntime().exec("git rev-parse --short=6 HEAD").inputStream.readAllBytes()
            gitVersion = new String(gitVersion, StandardCharsets.US_ASCII).strip()

            def data = Runtime.getRuntime().exec("git status --porcelain").inputStream.readAllBytes()
            if (data.length != 0) {
                // dirty
                gitVersion += "-dirty"
            }
            versionName += "-$gitVersion"
        } catch (ignored) {
        }

        versionName += "-${ranEmoji()}"

        return [versionCode, versionName]
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
                Matcher matcher = Pattern.compile("Pkg.Revision ?= ?(\\d+\\.\\d+(\\.\\d+)?)").matcher(line)
                if (matcher.find()) {
                    versionString = matcher.group(1)
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

        synchronized File getCmakeBinDir() {
            return ApacheFileUtils.getFile(new File(sdkDir as String),
                    "cmake", getCMakeVersion() as String, "bin")
        }
    }

    static Properties openPropertiesFile(File file) {
        def properties = new Properties()

        file.withReader("UTF-8") {
            properties.load(it)
        }

        return properties
    }

    static File getOpensslDir(File configPropertiesFile) {
        def key = "opensslLib.dir"

        if (!configPropertiesFile.exists()) {
            Assertion.doAssertion(configPropertiesFile.createNewFile())
        }
        def properties = openPropertiesFile(configPropertiesFile)
        if (!properties.containsKey(key)) {
            throw new GradleException("Could not find OpenSSL library. Please define \"$key\" in $configPropertiesFile.path")
        }

        def opensslDir = new File(properties.getProperty(key))
        if (!opensslDir.exists()) {
            throw new GradleException("\"$key\" defined in $configPropertiesFile.path doesn't exist")
        }
        return opensslDir
    }

    static class UnreachableError extends Error {}

    static enum TargetAbi {
        ARM_V7,
        ARM_V8,
        X86,
        X86_64

        @Override
        String toString() {
            switch (this) {
                case ARM_V7:
                    return "armeabi-v7a"
                case ARM_V8:
                    return "arm64-v8a"
                case X86:
                    return "x86"
                case X86_64:
                    return "x86_64"
                default:
                    throw new UnreachableError()
            }
        }

        static TargetAbi from(String name) {
            switch (name) {
                case "armeabi-v7a":
                    return ARM_V7
                case "arm64-v8a":
                    return ARM_V8
                case "x86_64":
                    return X86_64
                case "x86":
                    return X86
                default:
                    throw new GradleException("Unknown target ABI: $name")
            }
        }
    }

    static class OpensslPath {
        File lib
        File include

        OpensslPath(File lib, File include) {
            this.lib = lib
            this.include = include
        }
    }

    static getRustOpensslBuildEnv(String rustTargetString) {
        def prefix = rustTargetString.toUpperCase().replace('-', '_')
        return [includeDir: "${prefix}_OPENSSL_INCLUDE_DIR", libDir: "${prefix}_OPENSSL_LIB_DIR"]
    }

    static OpensslPath getOpensslPath(File opensslDir, TargetAbi targetAbi) {
        new OpensslPath(new File(new File(opensslDir, "libs"), targetAbi.toString()),
                new File(opensslDir, "include"))
    }
}
