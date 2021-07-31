package pers.zhc.plugins

import pers.zhc.util.IOUtils

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.util.regex.Pattern

/**
 * @author bczhc
 */
object BuildUtils2 {
  def checkGitVersion(): String = {
    val runtime = Runtime.getRuntime
    val exec = runtime.exec(Array("git", "--version"))
    val is = exec.getInputStream
    val read = readIS(is)
    is.close()

    val pattern = Pattern.compile("git version ([0-9]+\\.[0-9]+\\.[0-9]+)")
    val matcher = pattern.matcher(read)
    val find = matcher.find()
    if (!find) {
      throw new RuntimeException("Git not found")
    }
    matcher.group(1)
  }

  def readIS(is: InputStream): String = {
    val out = new ByteArrayOutputStream()
    IOUtils.streamWrite(is, out)
    out.toString()
  }

  def getGitCommitLog(path: File): String = {
    val runtime = Runtime.getRuntime
    val envp = Array.empty[String]
    val is = runtime.exec(Array("git", "log"), envp, path).getInputStream
    val read = readIS(is)
    is.close()

    read
  }
}