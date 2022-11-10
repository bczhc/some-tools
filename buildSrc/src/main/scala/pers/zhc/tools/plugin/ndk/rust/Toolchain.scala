package pers.zhc.tools.plugin.ndk.rust

import pers.zhc.tools.plugin.ndk.AndroidAbi
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

  val arName: String = "llvm-ar"
  val linker: File =
    new File(binDir, s"$prefix$androidApi-clang")
  val ar: File = new File(binDir, arName)

  require(linker.exists())
  require(ar.exists())
}
