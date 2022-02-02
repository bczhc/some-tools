package pers.zhc.tools.plugin.util

/** @author
  *   bczhc
  */
object ProcessUtils {
  def executeWithOutput(
      process: Process
  ): Int = {
    ProcessOutput.output(process)
    process.waitFor()
    process.exitValue()
  }
}
