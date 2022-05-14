package pers.zhc.tools.plugin.util

import pers.zhc.util.IOUtils

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.charset.StandardCharsets
import org.apache.commons.io.{FileUtils => ApacheFileUtils}

/** @author
  *   bczhc
  */
object FileUtils {
  def copyFile(src: File, dest: File): Unit = {
    val is = new FileInputStream(src)
    val os = new FileOutputStream(dest)

    IOUtils.streamWrite(is, os)

    is.close()
    os.close()
  }

  def getFileExtensionName(file: File): Option[String] = {
    val name = file.getName
    val index = name.lastIndexOf('.')
    if (index == -1) {
      None
    } else {
      Some(name.substring(index + 1))
    }
  }

  def writeFile(file: File, string: String): Unit = {
    val os = new FileOutputStream(file)
    os.write(string.getBytes(StandardCharsets.UTF_8))
    os.flush()
    os.close()
  }

  def requireDelete(file: File): Unit = {
    if (file.exists()) {
      if (file.isDirectory) {
        ApacheFileUtils.deleteDirectory(file)
      } else {
        ApacheFileUtils.delete(file)
      }
    }
  }

  def requireMkdir(file: File): Unit = {
    if (!file.exists()) {
      ApacheFileUtils.forceMkdir(file)
    }
  }
}
