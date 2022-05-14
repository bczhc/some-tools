import pers.zhc.plugins.MyPlugin1
import pers.zhc.plugins.MyPlugin2

apply {
    plugin(MyPlugin1::class)
    plugin(MyPlugin2::class)
}

buildscript {
    repositories {
        mavenCentral()
        google()
        maven {
            setUrl("https://jitpack.io")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
