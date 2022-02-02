package pers.zhc.tools.plugin.rust

import java.io.File

/** @author
  *   bczhc
  */
class Toolchain(val ndkPath: File, val androidApi: Int, val target: String) {
  private val binDir = ToolchainUtils.getToolchainBinDir(ndkPath)
  private val prefix = s"$target-linux-android"
  val linker: File = new File(binDir, s"$prefix$androidApi-clang")
  val ar: File = new File(binDir, s"$prefix-ar")

  require(linker.exists())
  require(ar.exists())
}
