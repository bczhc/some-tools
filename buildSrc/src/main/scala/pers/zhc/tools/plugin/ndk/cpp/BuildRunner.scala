package pers.zhc.tools.plugin.ndk.cpp

import pers.zhc.tools.plugin.ndk.Target
import pers.zhc.tools.plugin.ndk.cpp.CppBuildPlugin.Configurations
import pers.zhc.tools.plugin.util.{FileUtils, ProcessUtils}
import org.apache.commons.io.{FileUtils => ApacheFileUtils}

import java.io.File

class BuildRunner(configs: Configurations) {
  def run(): Unit = {
    val cmakeToolchainFile =
      new File(configs.ndkDir, "build/cmake/android.toolchain.cmake")
    if (!cmakeToolchainFile.exists()) {
      throw new RuntimeException(
        s"CMake toolchain file not found: ${cmakeToolchainFile.getAbsolutePath}"
      )
    }

    ApacheFileUtils.forceMkdirParent(configs.jniLibDir)

    val buildDir = new File(configs.srcDir, "build")

    for (target <- configs.targets) {
      println(s"Compiling for target: $target")
      runBuild(cmakeToolchainFile, target, buildDir)
      copyFiles(buildDir, configs.jniLibDir, target)
    }
  }

  private def copyFiles(
      buildDir: File,
      jniLibDir: File,
      target: Target
  ): Unit = {
    buildDir.listFiles().filter(_.getName.endsWith(".so")).foreach { soFile =>
      val destFile =
        ApacheFileUtils.getFile(jniLibDir, target.abi.toString, soFile.getName)

      ApacheFileUtils.copyFile(soFile, destFile)
    }
  }

  private def runBuild(
      cmakeToolchainFile: File,
      target: Target,
      buildDir: File
  ): Unit = {
    FileUtils.requireMkdir(buildDir)

    if (!new File(buildDir, "build.ninja").exists()) {
      val cmakeCommand = List(
        s"${configs.cmakeBinDir}/cmake",
        s"-DCMAKE_TOOLCHAIN_FILE=${cmakeToolchainFile.getAbsolutePath}",
        s"-DANDROID_ABI=${target.abi}",
        s"-DANDROID_PLATFORM=${target.api}",
        "-G",
        "Ninja",
        s"-DCMAKE_BUILD_TYPE=${configs.buildType.toString.capitalize}",
        s"-DCMAKE_MAKE_PROGRAM=${configs.cmakeBinDir}/ninja",
        configs.srcDir.getAbsolutePath
      )

      ProcessUtils.systemAndCheck(
        new ProcessBuilder(cmakeCommand: _*)
          .directory(buildDir)
          .start()
      )
    }

    val makeCommand =
      List(
        s"${configs.cmakeBinDir}/ninja",
        s"-j${Runtime.getRuntime.availableProcessors()}"
      )
    ProcessUtils.systemAndCheck(
      new ProcessBuilder(makeCommand: _*)
        .directory(buildDir)
        .start()
    )
  }
}
