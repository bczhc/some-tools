package pers.zhc.tools.plugin.ndk.cpp

import org.apache.commons.io.{FileUtils => ApacheFileUtils}
import pers.zhc.tools.plugin.ndk.Target
import pers.zhc.tools.plugin.ndk.cpp.CppBuildPlugin.Configurations
import pers.zhc.tools.plugin.util.ProcessUtils

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

    ApacheFileUtils.forceMkdir(configs.jniLibDir)

    for (target <- configs.targets) {
      val buildDir = new File(configs.srcDir, "build")
      val targetBuildDir = new File(buildDir, target.abi.toString)

      ApacheFileUtils.forceMkdir(targetBuildDir)

      val cmakeDefs = configs.cmakeDefs
        .map({ x =>
          x(target.abi.toString())
        })
        .getOrElse(Map())

      println(s"Compiling for target: $target")
      runBuild(cmakeToolchainFile, target, targetBuildDir, cmakeDefs)
      copyFiles(targetBuildDir, configs.jniLibDir, target)
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
      buildDir: File,
      cmakeDefs: Map[String, String]
  ): Unit = {
    if (!new File(buildDir, "build.ninja").exists()) {
      var cmakeCommand = List(
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
      cmakeCommand = cmakeCommand.patch(
        cmakeCommand.length - 1,
        cmakeDefs.map({ e => s"-D${e._1}=${e._2}" }),
        0
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
