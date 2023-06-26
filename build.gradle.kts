import pers.zhc.plugins.MyPlugin1
import pers.zhc.plugins.MyPlugin2

apply {
    plugin(MyPlugin1::class)
    plugin(MyPlugin2::class)
}

buildscript {
    dependencies {
        classpath("com.github.bczhc:android-native-build-plugin:c546ada670")
        classpath("org.apache.commons:commons-compress:1.23.0")
        classpath("org.tomlj:tomlj:1.1.0")
    }
}

plugins {
    id("com.android.application") version "7.3.0" apply false
    id("com.android.library") version "7.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
