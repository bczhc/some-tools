package pers.zhc.plugins

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class Utils {
    companion object {
        @JvmStatic
        fun copy(src: File, dest: File) {
            val buf = ByteArray(4096)
            var readLen: Int
            val inputStream = FileInputStream(src)
            val outputStream = FileOutputStream(dest, false)
            while (true) {
                readLen = inputStream.read(buf)
                if (readLen <= 0) break
                outputStream.write(buf, 0, readLen)
                outputStream.flush()
            }
            outputStream.close()
            inputStream.close()
        }

        @JvmStatic
        fun getJarSrcAndTargetLocation(project: Project): Array<File> {
            val rootProject = project.rootProject
            val javaProject = rootProject.project(":app:java")
            val appProject = rootProject.rootProject.project("app")

            val jar = javaProject.property("jar") as Jar
            val jarFile = jar.archiveFile.get().asFile
            if (!jarFile.exists()) {
                throw Exception("Jar archive generating failed.")
            }
            val jarTargetDir = File(appProject.buildDir, "jar-dep")
            if (!jarTargetDir.exists()) {
                jarTargetDir.mkdirs()
            }
            return arrayOf(jarFile, File(jarTargetDir, jarFile.name))
        }
    }
}