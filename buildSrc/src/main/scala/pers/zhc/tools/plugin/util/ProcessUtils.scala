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

  def systemAndCheck(
      process: Process
  ): Unit = {
    val status = executeWithOutput(process)
    if (status != 0) {
      throw new RuntimeException(
        s"Process exited with non-zero status: $status"
      )
    }
  }
}
