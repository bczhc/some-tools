package pers.zhc.plugins

import pers.zhc.util.IOUtils

import java.io._
import java.nio.charset.StandardCharsets
import java.util
import java.util.regex.Pattern
import scala.language.implicitConversions

/** @author
 * bczhc
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

  def getGitCommitLog(path: File): String = {
    val runtime = Runtime.getRuntime
    val envp = Array.empty[String]
    val is = runtime.exec(Array("git", "log"), envp, path).getInputStream
    val read = readIS(is)
    is.close()

    read
  }

  def readIS(is: InputStream): String = {
    val out = new ByteArrayOutputStream()
    IOUtils.streamWrite(is, out)
    out.toString(StandardCharsets.UTF_8)
  }

  def longStringToStringArray(str: String, splitLen: Int): String = {
    val sb = new StringBuilder("new String[] {\n")
    val split = splitString(str, splitLen)
    split.forEach(it => {
      sb.append('\"').append(it).append('\"').append(',').append('\n')
    })
    sb.append("}")
    sb.toString()
  }

  def splitString(string: String, length: Int): java.util.List[String] = {
    val list = new util.ArrayList[String]()
    val patternSB = new StringBuilder()
    var i = 0
    for (i <- 0 until length) {
      patternSB.append(".?")
    }

    val pattern = Pattern.compile(patternSB.toString())
    val matcher = pattern.matcher(string)
    while (matcher.find()) {
      val split = matcher.group()
      list.add(split)
    }
    list
  }

  implicit def whateverToRunnable[F](f: => F): Runnable = new Runnable() {
    def run(): Unit = {
      f
    }
  }

  def lzmaCompress(input: InputStream, output: OutputStream): Unit = {
    val process = new ProcessBuilder(List("xz", "--format=lzma"): _*).start()
    val pIS = process.getInputStream
    val pOS = process.getOutputStream
    new Thread({
      input.transferTo(pOS)
      pOS.close()
    }).start()

    pIS.transferTo(output)
    pIS.close()
    if (process.waitFor() != 0) {
      throw new RuntimeException("Non-zero exit status")
    }
  }

  def lzmaCompress(data: Array[Byte]): Array[Byte] = {
    val is = new ByteArrayInputStream(data)
    val os = new ByteArrayOutputStream()
    lzmaCompress(is, os)
    os.toByteArray
  }
}
