package pers.zhc.tools.plugin.rust

import pers.zhc.tools.plugin.rust.RustBuildPlugin.{BuildType, Configurations}
import pers.zhc.tools.plugin.util.ProcessUtils

import scala.jdk.CollectionConverters.SeqHasAsJava

/** @author
  *   bczhc
  */
class BuildRunner(toolchain: Toolchain, config: Configurations) {
  def run(): Int = {

    val runtime = Runtime.getRuntime
    val rustTarget = config.targetAbi.toRustTarget
    var command = List(
      "cargo",
      "build",
      "--target",
      rustTarget,
      s"-j${runtime.availableProcessors()}"
    )
    if (config.buildType == BuildType.Release) {
      command = command :+ "--release"
    }

    val pb = new ProcessBuilder(command.asJava)
    val env = pb.environment()
    env.put("TARGET_CC", toolchain.linker.getPath)
    env.put("TARGET_AR", toolchain.ar.getPath)
    config.extraEnv.foreach(_.foreach({ i => env.put(i._1, i._2) }))

    val targetEnvName = rustTarget.replace('-', '_').toUpperCase
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINKER",
      toolchain.linker.getPath
    )

    pb.directory(config.rustProjectDir)
    val progress = pb.start()

    ProcessUtils.executeWithOutput(progress)
  }
}
