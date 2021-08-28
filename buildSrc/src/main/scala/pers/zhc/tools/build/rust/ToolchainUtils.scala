package pers.zhc.tools.build.rust

import java.io.File

/**
 * @author bczhc
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

  def generateCargoConfig(rustTarget: String, toolchain: Toolchain): String = {
    val sb = new StringBuilder
    val newLine = '\n'
    sb.append(String.format("[target.%s]", rustTarget)).append(newLine)
      .append(String.format("linker = \"%s\"", toolchain.linker.getPath)).append(newLine)
      .append(String.format("ar = \"%s\"", toolchain.ar.getPath)).append(newLine)
    sb.toString()
  }
}
