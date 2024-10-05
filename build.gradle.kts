import pers.zhc.plugins.MyPlugin1
import pers.zhc.plugins.MyPlugin2

apply {
    plugin(MyPlugin1::class)
    plugin(MyPlugin2::class)
}

buildscript {
    dependencies {
        classpath("com.github.bczhc:android-native-build-plugin:e95ac75536")
        classpath("org.apache.commons:commons-compress:1.23.0")
        classpath("org.tomlj:tomlj:1.1.1")
        classpath("com.github.bczhc:android-native-build-plugin-config-parser:f4eee68fd2")
        classpath("com.github.bczhc:android-target-defs") {
            version {
                strictly("ac1ea2f9fc")
            }
        }
    }
}

plugins {
    val agpVersion = "8.6.0-alpha07"
    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
