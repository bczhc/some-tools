package pers.zhc.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File

class Test : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hi Kotlin.")
        val javaProject = project.project("java")

        val appProject = project.rootProject.project("app")
        val tasks = appProject.tasks

        tasks.register("buildJavaDepJar") { it ->
            it.dependsOn(":java:jar")
            it.doLast {
                val jar = javaProject.property("jar") as Jar
                val jarFile = jar.archiveFile.get().asFile
                if (!jarFile.exists()) {
                    throw Exception("Jar archive generating failed.")
                }
                val jarTargetDir = File(appProject.buildDir, "jar-dep")
                if (!jarTargetDir.exists()) {
                    jarTargetDir.mkdirs()
                }
                val jarTargetFile = File(jarTargetDir, jarFile.name)
                copy(jarFile, jarTargetFile)
                appProject.dependencies.add("implementation", appProject.files(jarTargetFile.path))
            }
        }
    }
}