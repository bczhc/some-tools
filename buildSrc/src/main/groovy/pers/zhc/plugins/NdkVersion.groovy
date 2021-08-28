package pers.zhc.plugins

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * @author bczhc
 */
class NdkVersion {
    static String readLocalProperties(File file) {
        if (!file.exists()) {
            return null
        }
        def properties = new Properties()
        def reader = file.newReader()
        properties.load(reader)
        reader.close()

        return properties.getProperty("ndk.version")
    }

    static String tryReadVersion(File ndkDir) {
        def propertiesFile = new File(ndkDir, "source.properties")
        def read = new String(propertiesFile.readBytes())
        def pattern = Pattern.compile("Pkg\\.Revision ?= ?([0-9]+\\.[0-9]+\\.[0-9]+)")
        def matcher = pattern.matcher(read)
        if (!matcher.find()) {
            return null
        }
        return matcher.group(1)
    }

    static String getLatestNdkVersion(File sdkPath) {
        // use the "ndk-bundle" NDK package in priority
        def ndkBundleVersion = getNdkBundleVersion(sdkPath)
        if (ndkBundleVersion != null) {
            return ndkBundleVersion
        }

        def ndkPath = new File(sdkPath, "ndk")
        if (!ndkPath.exists()) {
            return null
        }

        def versionStrings = []
        ndkPath.listFiles().toList().forEach {
            versionStrings.add(it.name)
        }

        def sortedVersionStrings = Version.sortVersionStrings(versionStrings as String[])
        if (sortedVersionStrings.length >= 1) {
            return sortedVersionStrings[0]
        }
        return null
    }

    static String readLocalProperties(Project project) {
        return readLocalProperties(new File(project.rootProject.projectDir, "local.properties"))
    }

    static String getNdkBundleVersion(File sdkPath) {
        try {
            def ndkBundlePath = new File(sdkPath, "ndk-bundle")
            def version = tryReadVersion(ndkBundlePath)
            if (version != null) {
                return version
            }
        } catch (FileNotFoundException ignored) {
        }
        return null
    }
}