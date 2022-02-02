package pers.zhc.tools.plugin.rust

import pers.zhc.tools.plugin.rust.RustBuildPlugin.Configurations
import pers.zhc.tools.plugin.util.ProcessUtils

import scala.jdk.CollectionConverters.SeqHasAsJava

/** @author
  *   bczhc
  */
class BuildRunner(toolchain: Toolchain, config: Configurations) {
  def run(): Int = {

    val runtime = Runtime.getRuntime
    val rustTarget = config.targetAbi.toRustTarget
    val command = List(
      "cargo",
      "build",
      "--target",
      rustTarget,
      s"-j${runtime.availableProcessors()}",
      s"--${config.buildType.toString}"
    )

    val pb = new ProcessBuilder(command.asJava)
    val env = pb.environment()
    env.put("TARGET_CC", toolchain.linker.getPath)
    env.put("TARGET_AR", toolchain.ar.getPath)
    config.extraEnv.foreach(_.foreach({ i => env.put(i._1, i._2) }))

    val targetEnvName = rustTarget.replace('-', '_').toUpperCase
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINUX_ANDROID_CC",
      toolchain.linker.getPath
    )
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINUX_ANDROID_AR",
      toolchain.ar.getPath
    )

    pb.directory(config.rustProjectDir)
    val progress = pb.start()

    ProcessUtils.executeWithOutput(progress)
  }
}
