@file:Suppress("UnstableApiUsage")

import pers.zhc.plugins.BuildUtils.*
import pers.zhc.plugins.FileUtils.requireCreate
import pers.zhc.plugins.NdkVersion
import pers.zhc.plugins.RegexUtils
import pers.zhc.plugins.SdkPath
import pers.zhc.tools.plugin.rust.AndroidAbi
import pers.zhc.tools.plugin.rust.RustBuildPlugin
import pers.zhc.tools.plugin.rust.RustBuildPlugin.RustBuildPluginExtension
import java.io.FileNotFoundException
import java.util.*
import pers.zhc.plugins.`BuildUtils2$`.`MODULE$` as BuildUtils2
import pers.zhc.tools.plugin.util.`FileUtils$`.`MODULE$` as FileUtils

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}
apply<RustBuildPlugin>()

val base64Encoder = Base64.getEncoder()!!
val commitLogResult = try {
    val gitVersion = BuildUtils2.checkGitVersion()!!
    println("Found git $gitVersion")

    try {
        val log = BuildUtils2.getGitCommitLog(projectDir)!!
        println("git log string size: ${log.toByteArray().size}")
        log.ifEmpty { "Unknown" }
    } catch (e: Exception) {
        throw GradleException("Execution failed", e)
    }
} catch (e: Exception) {
    if (e is GradleException) {
        throw e
    }

    println("Found git error: $e; disable git commit log injection")
    "Unknown"
}

val configPropertiesFile = File(rootDir, "config.properties")
requireCreate(configPropertiesFile)

val ndkFormatHintMsg = """Format: <ABI-name>-<native-API-version>
Example:
ndk.target=arm64-v8a-29,x86_64-29"""

val properties = openPropertiesFile(configPropertiesFile)!!
val propNdkTarget = properties.getProperty("ndk.target") ?: run {
    throw GradleException("Please define \"ndk.target\" in $configPropertiesFile.path\n$ndkFormatHintMsg")
}

val ndkTargets = propNdkTarget.split(',').map { it.trim() }.map {
    if (!it.matches(Regex("^.*-[0-9]+\$"))) {
        throw GradleException("Wrong NDK target format: $it\n$ndkFormatHintMsg")
    }
    val captured = RegexUtils.capture(it, "^(.*)-([0-9]+)\$")
    mapOf(
        Pair("abi", TargetAbi.from(captured[0][1])!!), Pair("api", captured[0][2].toInt())
    )
}


android {
    signingConfigs {
        val configs = asMap
        configs["debug"]!!.apply {
            storeFile = file("test.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
        configs["release"] = configs["debug"]!!
    }
    compileSdkVersion(31)
    defaultConfig {
        applicationId = "pers.zhc.tools"
        minSdkVersion(21)
        targetSdkVersion(31)

        val verInfo = gVersion()!! as ArrayList<*>
        versionCode(verInfo[0] as Int)
        versionName(verInfo[1].toString())

        ndk {
            abiFilters.addAll(ndkTargets.map {
                it["abi"].toString()
            })
        }

        buildConfigField(
            "String[]", "commitLogEncodedSplit", BuildUtils2.longStringToStringArray("a", 100)
        )
    }
    buildTypes {
        val types = asMap
        types["debug"]!!.apply {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules-debug.pro")
            isDebuggable = true
            isJniDebuggable = true
        }
        types["release"]!!.apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        isCheckReleaseBuilds = false
        isAbortOnError = false
    }


    val fontSdkDir = try {
        file(SdkPath.getSdkPath(project))
    } catch (_: FileNotFoundException) {
        throw GradleException("Cannot found sdk path. Please define \"sdk.dir\" in the project \"local.properties\"")
    }
    val detectedNdkVersion = NdkVersion.getLatestNdkVersion(fontSdkDir) ?: run {
        NdkVersion.readLocalProperties(project) ?: run {
            throw GradleException(
                "Cannot get NDK version. Please check NDK directory \"\$SDK/ndk/\" " + "or try to define \"ndk.version\" in the project \"local.properties\""
            )
        }
    }


    ndkVersion = detectedNdkVersion

    val sdkDir = android.sdkDirectory
    val ndkDir = android.ndkDirectory

    val tools = Tools(ndkDir, sdkDir)
    val cmakeVersion = tools.cMakeVersion as String

    externalNativeBuild {
        cmake {
            version = cmakeVersion
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    sourceSets {
        val sets = asMap
        sets["main"]!!.apply {
            jniLibs.srcDirs("jniLibs")
        }
    }
}


val appProject = project
val jniOutputDir = File(appProject.projectDir, "jniLibs").also { it.mkdirs() }

val opensslDir = getOpensslDir(configPropertiesFile)!!

val rustBuildExtraEnv = HashMap<String, String>()
ndkTargets.forEach {
    val env = (getRustOpensslBuildEnv(AndroidAbi.from(it["abi"].toString()).toRustTarget())
            as Map<*, *>).map { e ->
        Pair(e.key.toString(), e.value.toString())
    }.toMap()
    val opensslPath = getOpensslPath(opensslDir, it["abi"] as TargetAbi)
    rustBuildExtraEnv[env["libDir"].toString()] = opensslPath.lib!!.path
    rustBuildExtraEnv[env["includeDir"].toString()] = opensslPath.include!!.path
}

configure<RustBuildPluginExtension> {
    androidNdkDir.set(android.ndkDirectory.path)
    androidApi.set(21)
    targets.set(ndkTargets.map {
        mapOf(
            Pair("abi", it["abi"].toString()),
            Pair("api", it["api"])
        )
    })
    buildType.set("release")
    rustProjectDir.set(File(appProject.projectDir, "src/main/rust").path)
    outputDir.set(jniOutputDir.path)
    extraEnv.set(rustBuildExtraEnv)
}


val copyOpensslLibsTask = project.task("copyOpensslLibs") {
    doLast {
        ndkTargets.forEach {
            val abi = it["abi"] as TargetAbi
            val opensslPath = getOpensslPath(opensslDir, abi)
            listOf(
                "libssl.so",
                "libcrypto.so"
            ).map { libName ->
                File(opensslPath.lib!!, libName)
            }.forEach { file ->
                if (!file.exists()) {
                    throw GradleException("Required OpenSSL library file not found: ${file.path}")
                }

                val outputDir = File(jniOutputDir, abi.toString()).also { d -> d.mkdirs() }
                FileUtils.copyFile(file, File(outputDir, file.name))
            }
        }
    }
}


val compileRustTask: Task = appProject.tasks.getByName(RustBuildPlugin.TASK_NAME())

appProject.tasks.getByName("preBuild").dependsOn(compileRustTask)
compileRustTask.dependsOn(copyOpensslLibsTask)


println("""Build environment info:
    |SDK path: ${android.sdkDirectory.path}
    |NDK path: ${android.ndkDirectory.path}
    |CMake version: ${android.externalNativeBuild.cmake.version}
    |NDK targets: $ndkTargets
    |Rust build extra env: $rustBuildExtraEnv
""".trimMargin())


repositories {
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.4.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.github.mik3y:usb-serial-for-android:3.3.0")
    implementation("com.github.bczhc:java-lib:18a858c167")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    implementation("me.zhanghai.android.fastscroll:library:1.1.7")
    implementation("com.github.bczhc:jni-java:3a74b3d640")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
}

task("saveNdkPath") {
    doLast {
        val path = android.ndkDirectory.path
        File(rootDir, "tmp").writeText(path)
    }
}
