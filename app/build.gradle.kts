@file:Suppress("UnstableApiUsage")
@file:SuppressLint("JcenterRepositoryObsolete")

import android.annotation.SuppressLint
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream.MAX_BLOCKSIZE
import org.apache.commons.io.output.ByteArrayOutputStream
import org.tomlj.Toml
import pers.zhc.android.def.BuildType
import pers.zhc.gradle.plugins.ndk.*
import pers.zhc.gradle.plugins.ndk.cpp.CppBuildPlugin
import pers.zhc.gradle.plugins.ndk.cpp.CppBuildPlugin.CppBuildPluginExtension
import pers.zhc.gradle.plugins.ndk.rust.RustBuildPlugin
import pers.zhc.gradle.plugins.ndk.rust.RustBuildPlugin.RustBuildPluginExtension
import pers.zhc.plugins.BuildUtils.*
import pers.zhc.plugins.FileUtils.requireCreate
import pers.zhc.plugins.NdkVersion
import pers.zhc.plugins.SdkPath
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import pers.zhc.gradle.plugins.util.`FileUtils$`.`MODULE$` as FileUtils
import pers.zhc.plugins.`BuildUtils2$`.`MODULE$` as BuildUtils2

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
apply<RustBuildPlugin>()
apply<CppBuildPlugin>()

val rootProjectDir = rootProject.projectDir

val base64Encoder = Base64.getEncoder()!!
val commitLogResult = try {
    val gitVersion = BuildUtils2.checkGitVersion()!!
    println("Found git $gitVersion")

    try {
        val log = BuildUtils2.getGitCommitLog(projectDir)!!
        println("Git log string size: ${log.toByteArray().size}")
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

val buildConfigs = parseConfigTomlFile()
val ndkTargets = buildConfigs.ndk.targets

val disableRustBuild = !buildConfigs.rust.enableBuild
val rustKeepDebugSymbols = buildConfigs.rust.keepDebugSymbols

val foundSdkDir = try {
    file(SdkPath.getSdkPath(project))
} catch (_: FileNotFoundException) {
    throw GradleException("Cannot found sdk path. Please define \"sdk.dir\" in the project \"local.properties\"")
}
val detectedNdkVersion = NdkVersion.getLatestNdkVersion(foundSdkDir) ?: run {
    NdkVersion.readLocalProperties(project) ?: run {
        throw GradleException(
            "Cannot get NDK version. Please check NDK directory \"\$SDK/ndk/\" " + "or try to define \"ndk.version\" in the project \"local.properties\""
        )
    }
}
val foundNdkDir = Paths.get(foundSdkDir.path, "ndk", detectedNdkVersion)

data class GeneratedVersion(val code: Int, val name: String)
val verInfo = generateVersion()!! as ArrayList<*>
val generatedVersion = GeneratedVersion(verInfo[0] as Int, verInfo[1].toString())

val opensslShlibVariant = "-bundled"

val appProject = project
val jniOutputDir = File(appProject.projectDir, "jniLibs").also { it.mkdirs() }

val opensslDir = buildConfigs.opensslDir

val rustBuildExtraEnv = HashMap<String, String>()
val rustBuildTargetEnv = HashMap<String, Map<String, String>>()

val copyOpensslLibsTask = project.task("copyOpensslLibs") {
    doLast {
        ndkTargets.forEach {
            val abi = it.abi
            val opensslPath = getOpensslPath(opensslDir, abi)
            listOf(
                "libssl$opensslShlibVariant.so", "libcrypto$opensslShlibVariant.so"
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

var compileRustTask: Task? = null
if (!disableRustBuild) {
    compileRustTask = appProject.tasks.getByName(RustBuildPlugin.TASK_NAME())
}

val sdkDir: File = foundSdkDir
val ndkDir: File = foundNdkDir.toFile()
val tools = Tools(ndkDir, sdkDir)
val cmakeVersion = tools.cMakeVersion as String

val cmakeDefsMap = HashMap<String, Map<String, String>>()
ndkTargets.forEach {
    val abi = it.abi
    val opensslPath = getOpensslPath(opensslDir, abi)
    cmakeDefsMap[abi.toString()] = mapOf(
        Pair("OPENSSL_INCLUDE_DIR", opensslPath.include.path),
        Pair("OPENSSL_LIBS_DIR", opensslPath.lib.path),
        Pair("OPENSSL_CRYPTO_LINK_SONAME", "crypto$opensslShlibVariant"),
        Pair("OPENSSL_SSL_LINK_SONAME", "ssl$opensslShlibVariant"),
    )
}

var buildInfoMessage = """Version code: ${generatedVersion.code}
    |Version name: ${generatedVersion.name}
    |Rust build enabled: ${buildConfigs.rust.enableBuild}
    |Rust JNI enabled: ${buildConfigs.rust.enableJni}
    |NDK build type: ${buildConfigs.ndk.buildType}
    |SDK path: $sdkDir
    |NDK path: $ndkDir
    |NDK version: $detectedNdkVersion
    |CMake version: $cmakeVersion
    |NDK targets: $ndkTargets
    |CMake -D variables: $cmakeDefsMap
""".trimMargin() + "\n"

if (!disableRustBuild) {
    buildInfoMessage += """Rust build extra env: $rustBuildExtraEnv
    |Rust build target env: $rustBuildTargetEnv
""".trimMargin()
}
println("=========== Build info ===========")
println(buildInfoMessage)

android {
    namespace = "pers.zhc.tools"
    signingConfigs {
        val configs = asMap
        configs["debug"]!!.apply {
            storeFile = file("test.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }
    compileSdk = 34
    defaultConfig {
        applicationId = "pers.zhc.tools"
        minSdk = 21
        targetSdk = 34


        versionCode = generatedVersion.code
        versionName = generatedVersion.name

        ndk {
            abiFilters.addAll(buildConfigs.ndk.targets.map { it.abi.name })
        }

        val compressedGitLog = bzip2Compress(commitLogResult.toByteArray(Charsets.UTF_8))

        buildConfigField(
            "String[]", "commitLogEncodedSplit", BuildUtils2.longStringToStringArray(
                base64Encoder.encodeToString(compressedGitLog),
                100
            )
        )
        buildConfigField("boolean", "rustEnableBuild", buildConfigs.rust.enableBuild.toString())
        buildConfigField("boolean", "rustEnableJni", buildConfigs.rust.enableJni.toString())
        buildConfigField("boolean", "ndkReleaseBuild", "${buildConfigs.ndk.buildType == BuildType.RELEASE}")
        buildConfigField(
            "String[]",
            "buildInfoMessageEncoded",
            BuildUtils2.longStringToStringArray(base64Encoder.encodeToString(buildInfoMessage.toByteArray()), 100)
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        val types = asMap
        types["debug"]!!.apply {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules-debug.pro")
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
        types["release"]!!.apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
//    lint {
//        isCheckReleaseBuilds = false
//        isAbortOnError = false
//    }

    ndkVersion = detectedNdkVersion

    sourceSets {
        val sets = asMap
        sets["main"]!!.apply {
            jniLibs.srcDirs("jniLibs")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    packagingOptions {
        if (rustKeepDebugSymbols) {
            jniLibs.keepDebugSymbols += "**/librust_jni.so"
        }
    }
}

if (!disableRustBuild) {
    ndkTargets.forEach {
        val env = (getRustOpensslBuildEnv(it.abi.toRustTriple()) as Map<*, *>).map { e ->
            Pair(e.key.toString(), e.value.toString())
        }.toMap()
        val opensslPath = getOpensslPath(opensslDir, it.abi)
        rustBuildExtraEnv[env["libDir"].toString()] = opensslPath.lib!!.path
        rustBuildExtraEnv[env["includeDir"].toString()] = opensslPath.include!!.path
    }
    rustBuildExtraEnv["OPENSSL_LIBS"] = "ssl$opensslShlibVariant:crypto$opensslShlibVariant"

    ndkTargets.forEach {
        val abi = it.abi.name
        rustBuildTargetEnv[abi] = HashMap<String, String>().apply {
            this["SQLITE3_INCLUDE_DIR"] =
                "$projectDir/app/src/main/cpp/third_party/jni-lib/third_party/my-cpp-lib/third_party/sqlite3-single-c"
            this["SQLITE3_LIB_DIR"] = File(jniOutputDir, abi).path
        }
    }

    configure<RustBuildPluginExtension> {
        ndkDir.set(android.ndkDirectory.path)
        targets.set(GradleExtensionConfigConverters.targetsToMap(ndkTargets))
        buildType.set(buildConfigs.ndk.buildType.toString())
        srcDir.set(File(appProject.projectDir, "src/main/rust").path)
        outputDir.set(jniOutputDir.path)
        extraEnv.set(rustBuildExtraEnv)
        targetEnv.set(rustBuildTargetEnv)
    }
}

configure<CppBuildPluginExtension> {
    srcDir.set("$projectDir/src/main/cpp")
    ndkDir.set(android.ndkDirectory.path)
    targets.set(GradleExtensionConfigConverters.targetsToMap(ndkTargets))
    buildType.set(buildConfigs.ndk.buildType.name)
    outputDir.set(jniOutputDir.path)
    cmakeBinDir.set(tools.cmakeBinDir.path)
    cmakeDefs.set(cmakeDefsMap)
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:5.1.0")
    implementation("com.google.android.material:material:1.8.0-alpha03")
    implementation("com.github.mik3y:usb-serial-for-android:3.3.0")
    implementation("com.github.bczhc:java-lib:18a858c167")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("me.zhanghai.android.fastscroll:library:1.1.8")
    implementation("com.github.bczhc:jni-java:1c894fd591")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.github.reddit:IndicatorFastScroll:1.4.0")
    implementation("com.quiph.ui:recyclerviewfastscroller:1.0.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.2")
    implementation(kotlin("reflect"))
    implementation("com.github.thellmund.Android-Week-View:core:5.3.2")
    implementation("org.apache.commons:commons-compress:1.23.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

task("saveNdkPath") {
    doLast {
        val path = android.ndkDirectory.path
        File(rootDir, "tmp").writeText(path)
    }
}

val cleanAllTask = rootProject.task("cleanAll") {
    if (disableRustBuild) {
        dependsOn("clean", ":app:cleanCpp")
    } else {
        dependsOn("clean", ":app:cleanRust", ":app:cleanCpp")
    }
}
cleanAllTask.doLast {
    listOf(
        buildDir,
        rootProject.buildDir,
        jniOutputDir,
        File(rootProjectDir, ".gradle"),
        File(rootProjectDir, "app/.cxx"),
        File(rootProjectDir, "buildSrc/build"),
        File(rootProjectDir, "buildSrc/.gradle")
    ).forEach { requireDelete(it) }
}

fun requireDelete(file: File) {
    if (file.exists()) {
        if (!delete(file)) {
            throw IOException("Failed to delete ${file.path}")
        }
    }
}

val compileCppTask: Task = project.tasks.getByName("compileCpp")

val compileJniTask = task("compileJni") {
    compileCppTask.dependsOn(copyOpensslLibsTask)
    dependsOn(compileCppTask)

    if (!disableRustBuild) {
        dependsOn(compileRustTask!!)
    }
}

appProject.tasks.getByName("preBuild").dependsOn(compileJniTask)

fun bzip2Compress(data: ByteArray): ByteArray {
    val compressed = ByteArrayOutputStream()
    val compressor = BZip2CompressorOutputStream(compressed, MAX_BLOCKSIZE)
    compressor.write(data)
    compressor.close()
    return compressed.toByteArray()
}

fun parseConfigTomlFile(): BuildConfigs {
    val configTomlFile = File(rootDir, "config.toml")

    if (!configTomlFile.exists()) {
        requireCreate(configTomlFile)
    }

    val parsed = ConfigParser.parse(configTomlFile)

    val configToml = Toml.parse(configTomlFile.reader())!!

    configToml.errors().forEach {
        throw GradleException("`config.toml` parsing error: $it")
    }

    return BuildConfigs(
        opensslDir = File(configToml.requireString("ndk.openssl_dir")),
        ndk = parsed.ndk,
        rust = RustConfigs(
            enableBuild = configToml.getBoolean("ndk.rust.enable_build") ?: true,
            enableJni = configToml.getBoolean("ndk.rust.enable_jni") ?: true,
            keepDebugSymbols = configToml.getBoolean("ndk.rust.keep_debug_symbols") ?: false,
        ),
    )
}

data class BuildConfigs(
    val opensslDir: File,
    val ndk: ConfigParser.NdkConfig,
    val rust: RustConfigs,
)

data class RustConfigs(
    val enableBuild: Boolean,
    val enableJni: Boolean,
    val keepDebugSymbols: Boolean,
)
