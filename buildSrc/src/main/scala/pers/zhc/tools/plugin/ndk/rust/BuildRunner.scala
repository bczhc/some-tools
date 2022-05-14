package pers.zhc.tools.plugin.ndk.rust

import pers.zhc.tools.plugin.ndk.rust.BuildRunner.BuildOptions
import pers.zhc.tools.plugin.ndk.rust.RustBuildPlugin.Environments
import pers.zhc.tools.plugin.ndk.{BuildType, Target}
import pers.zhc.tools.plugin.util.ProcessUtils

import java.io.File
import scala.jdk.CollectionConverters.SeqHasAsJava

/** @author
  *   bczhc
  */
class BuildRunner(toolchain: Toolchain, options: BuildOptions) {
  def run(): Int = {

    val runtime = Runtime.getRuntime
    val rustTarget = options.target.abi.toRustTarget
    var command = List(
      "cargo",
      "build",
      "--target",
      rustTarget,
      s"-j${runtime.availableProcessors()}"
    )
    if (options.buildType == BuildType.Release) {
      command = command :+ "--release"
    }

    val pb = new ProcessBuilder(command.asJava)
    val env = pb.environment()
    env.put("TARGET_CC", toolchain.linker.getPath)
    env.put("TARGET_AR", toolchain.ar.getPath)
    options.extraEnv.foreach(_.foreach({ i => env.put(i._1, i._2) }))

    val targetEnvName = rustTarget.replace('-', '_').toUpperCase
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINKER",
      toolchain.linker.getPath
    )

    pb.directory(options.rustProjectDir)
    val progress = pb.start()

    ProcessUtils.executeWithOutput(progress)
  }
}

object BuildRunner {
  case class BuildOptions(
      target: Target,
      buildType: BuildType,
      extraEnv: Option[Environments],
      rustProjectDir: File
  )
}
