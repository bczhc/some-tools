package pers.zhc.plugins

import org.gradle.api.Project

/**
 * @author bczhc
 */
class SdkPath {
    static String getSdkPath(Project project) {
        def propertiesFile = new File(project.rootProject.projectDir, "local.properties")
        if (!propertiesFile.exists()) {
            return null
        }
        def properties = BuildUtils.openPropertiesFile(propertiesFile)
        def sdkDir = properties.getProperty("sdk.dir")
        return sdkDir
    }
}
