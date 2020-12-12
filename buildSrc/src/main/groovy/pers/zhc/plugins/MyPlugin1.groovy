package pers.zhc.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author bczhc
 */
class MyPlugin1 implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println("hello, world")
    }
}