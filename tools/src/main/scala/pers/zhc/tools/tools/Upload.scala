package pers.zhc.tools.tools

import org.apache.commons.io.FileUtils
import org.json.{JSONArray, JSONObject}
import pers.zhc.util.IOUtils

import java.io._
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util
import java.util.Base64
import scala.util.control.Breaks.{break, breakable}

/**
 * @author bczhc
 */
object Upload {

  // Usage: program <apk-file> <encoded-commit-info>
  def main(args: Array[String]): Unit = {
    require(args.length == 2)

    val apkFile = new File(args(0))
    val commitInfo = base64DecodeToString(args(1))

    val apkDirFile = new File("./apks/some-tools")
    if (!apkDirFile.exists()) {
      require(apkDirFile.mkdirs())
    }

    val commitHash = findCommitHash(commitInfo)
    if (checkCommitExistence(apkDirFile, commitHash)) {
      return
    }

    val jsonFile = new File("./apks/some-tools/log.json")
    if (!jsonFile.exists()) {
      val os = new FileOutputStream(jsonFile)
      os.write("[]".getBytes)
      os.flush()
      os.close()
    }
    assert(jsonFile.exists())

    val jsonArray = new JSONArray(readFileToString(jsonFile))
    val newCommit = createGitCommitJSON(commitInfo, computeFileSha1String(apkFile))
    val commits = new util.ArrayList[JSONObject]()
    for (i <- 0 until jsonArray.length()) {
      commits.add(jsonArray.getJSONObject(i))
    }
    commits.add(0, newCommit)

    // keep the latest 10 records
    val kept = new Array[JSONObject](math.min(commits.size(), 10))
    for (i <- kept.indices) {
      kept(i) = commits.get(i)
    }
    jsonArray.clear()
    jsonArray.putAll(kept)
    writeStringToFile(jsonFile, jsonArray.toString(2))

    clearDirs(kept, apkDirFile)

    val newApkFileDir = new File(apkDirFile, commitHash)
    if (!newApkFileDir.exists()) {
      require(newApkFileDir.mkdir())
    }
    val newApkFile = new File(newApkFileDir, apkFile.getName)
    copyFile(apkFile, newApkFile)
  }

  def readFileToString(file: File): String = {
    val baos = new ByteArrayOutputStream()
    val is = new FileInputStream(file)

    bufferedReadBytes(is, (bytes, readLen) => {
      baos.write(bytes, 0, readLen)
    })

    is.close()
    baos.toString("UTF-8")
  }

  def createGitCommitJSON(commitInfo: String, fileSha1: String): JSONObject = {
    val json = new JSONObject()
    json.put("commitInfo", commitInfo)
    json.put("fileSha1", fileSha1)
    json
  }

  def base64DecodeToString(encoded: String): String = {
    val bytes = Base64.getDecoder.decode(encoded)
    new String(bytes, StandardCharsets.UTF_8)
  }

  def clearDirs(kept: Array[JSONObject], apkDirFile: File): Unit = {
    val files = apkDirFile.listFiles()
    val exist = (name: String) => {
      var exist = false
      breakable {
        kept.foreach(o => {
          if (findCommitHash(o.getString("commitInfo")) == name) {
            exist = true
            break()
          }
        })
      }
      exist
    }
    files.withFilter(f => f.isDirectory).foreach(f => {
      if (!exist(f.getName)) {
        FileUtils.deleteDirectory(f)
      }
    })
  }

  def copyFile(src: File, dest: File): Unit = {
    val is = new FileInputStream(src)
    val os = new FileOutputStream(dest)
    IOUtils.streamWrite(is, os)
    os.close()
    is.close()
  }

  def computeFileSha1(f: File): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA1")
    val is = new FileInputStream(f)
    bufferedReadBytes(is, (bytes, readLen) => {
      md.update(bytes, 0, readLen)
    })
    md.digest()
  }

  def computeFileSha1String(f: File): String = {
    bytes2hexString(computeFileSha1(f))
  }

  def bytes2hexString(bytes: Array[Byte]): String = {
    val digestString = new StringBuilder
    for (b <- bytes) {
      val str = Integer.toHexString(if (b < 0) 256 + b else b)
      digestString.append(if (str.length == 1) s"0$str" else str)
    }
    digestString.toString()
  }

  def bufferedReadBytes(is: InputStream, f: (Array[Byte], Int) => Unit): Unit = {
    val buf = new Array[Byte](4096)
    var readLen = 0
    breakable {
      while (true) {
        readLen = is.read(buf)
        if (readLen == -1) {
          break()
        }
        f(buf, readLen)
      }
    }
  }

  def writeStringToFile(f: File, s: String): Unit = {
    val os = new FileOutputStream(f)
    os.write(s.getBytes(StandardCharsets.UTF_8))
    os.flush()
    os.close()
  }

  def checkCommitExistence(apkDirFile: File, commit: String): Boolean = {
    val files = apkDirFile.listFiles()
    var exist = false
    breakable {
      files.foreach(f => {
        if (f.isDirectory && f.getName == commit) {
          exist = true
          break()
        }
      })
    }
    exist
  }

  def findCommitHash(commitInfo: String): String = {
    val firstLine = commitInfo.split('\n')(0)
    val prefix = "commit "
    val i = firstLine.indexOf(prefix)
    require(i != -1)
    val hash = firstLine.substring(i + prefix.length)
    require(hash.length == 40)
    hash
  }
}
