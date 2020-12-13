package pers.zhc.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File

class MyPlugin2 : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hi Kotlin.")
        val tasks = project.tasks

        tasks.register("buildJavaDepJar") {
            it.dependsOn("java:jar")
            it.doLast {
                val jarProperties = Utils.getJarSrcAndTargetLocation(project)
                Utils.copy(jarProperties[0], jarProperties[1])
            }
        }
    }
}