package pers.zhc.tools.plugin.ndk.rust

import java.io.File

/** @author
  *   bczhc
  */
object ToolchainUtils {
  def getToolchainBinDir(ndkPath: File): File = {
    val prebuiltDir = new File(ndkPath, "toolchains/llvm/prebuilt")
    val listFiles = prebuiltDir.listFiles(filter => filter.isDirectory)
    if (listFiles.length != 1) {
      throw new RuntimeException("Multiple host directories exist")
    }
    new File(listFiles(0), "bin")
  }
}
