package pers.zhc.tools.plugin.rust

import java.io.File

/** @author
  *   bczhc
  */
class Toolchain(
    val ndkPath: File,
    val androidApi: Int,
    val androidAbi: AndroidAbi
) {
  private val binDir = ToolchainUtils.getToolchainBinDir(ndkPath)
  private val prefix: String = androidAbi.toNdkToolchainName

  val arName: String = androidAbi match {
    case AndroidAbi.Arm    => "arm-linux-androideabi-ar"
    case AndroidAbi.Arm64  => "aarch64-linux-android-ar"
    case AndroidAbi.X86    => "i686-linux-android-ar"
    case AndroidAbi.X86_64 => "x86_64-linux-android-ar"
  }
  val linker: File =
    new File(binDir, s"$prefix$androidApi-clang")
  val ar: File = new File(binDir, arName)

  require(linker.exists())
  require(ar.exists())
}
