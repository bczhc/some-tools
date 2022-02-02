package pers.zhc.tools.plugin.rust

import org.gradle.api.provider.Property
import org.gradle.api.{GradleException, Plugin, Project, Task}
import pers.zhc.tools.plugin.rust.RustBuildPlugin.RustBuildPluginExtension
import pers.zhc.tools.plugin.util.{FileUtils, ProcessOutput}

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.jdk.CollectionConverters.SeqHasAsJava

class RustBuildPlugin extends Plugin[Project] {
  var appProjectDir: Option[File] = None
  var extension: Option[RustBuildPluginExtension] = None
  var project: Option[Project] = None

  override def apply(project: Project): Unit = {
    project.task(
      "compileReleaseRust",
      { task: Task =>
        val extension = project.getExtensions
          .create("rustBuild", classOf[RustBuildPluginExtension])
        this.extension = Some(extension)

        task.doLast { _: Task =>
          checkRequiredExtension(extension)
          compileReleaseRust()
        }
        ()
      }
    )

    val appProject = project.getRootProject.findProject("app")
    require(appProject != null)
    appProjectDir = Some(appProject.getProjectDir)

    this.project = Some(project)
  }

  def executeProgram(
      process: Process
  ): Int = {
    ProcessOutput.output(process)
    process.waitFor()
    process.exitValue()
  }

  def compileReleaseRust(): Unit = {
    assert(appProjectDir.isDefined)
    assert(extension.isDefined)

    val architecture = getTarget
    val rustTarget = architecture

    val rustProjectDir = getRustProjectDir
    val jniLibsDir = getJniLibsDir

    val runtime = Runtime.getRuntime
    var command = List(
      "cargo",
      "build",
      "--target",
      rustTarget,
      "-j",
      s"${runtime.availableProcessors()}"
    )
    val buildType = getBuildType
    if (buildType == BuildType.Release) {
      command = command :+ "--release"
    }

    val opensslDir = getOpensslDir
    val opensslLibDir = getOpensslLibDir
    val cryptoLibFile = new File(opensslLibDir, "libcrypto.so")
    val sslLibFile = new File(opensslLibDir, "libssl.so")
    if (
      !opensslDir.exists() || !opensslLibDir.exists() || !cryptoLibFile
        .exists() || !sslLibFile.exists()
    ) {
      throw new GradleException(
        s"Couldn't find \"${cryptoLibFile.getPath}\" and \"${sslLibFile.getPath}\""
      )
    }

    val toolchain = getToolchain

    val pb = new ProcessBuilder(command.asJava)
    val env = pb.environment()
    env.put("OPENSSL_DIR", opensslDir.getPath)
    env.put("TARGET_CC", toolchain.linker.getPath)
    env.put("TARGET_AR", toolchain.ar.getPath)
    val targetEnvName = rustTarget.replace('-', '_').toUpperCase
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINUX_ANDROID_CC",
      toolchain.linker.getPath
    )
    env.put(
      s"CARGO_TARGET_${targetEnvName}_LINUX_ANDROID_AR",
      toolchain.ar.getPath
    )

    pb.directory(rustProjectDir)
    val progress = pb.start()

    val status = executeProgram(progress)
    if (status != 0) {
      throw new GradleException(
        "Failed to run program: exits with non-zero return value"
      )
    }

    val rustOutputDir =
      new File(rustProjectDir, s"target/$architecture/${buildType.toString}")
    assert(rustOutputDir.exists())

    if (!jniLibsDir.exists()) {
      require(jniLibsDir.mkdir())
    }

    val listSoFiles: (File, File => Unit) => Unit = { (dir, func) =>
      dir.listFiles().foreach { f: File =>
        if (
          f.isFile && FileUtils.getFileExtensionName(f).getOrElse("") == "so"
        ) {
          func(f)
        }
      }
    }

    val outputSoDir = {
      val d = new File(jniLibsDir, getAndroidAbi.toString)
      if (!d.exists()) {
        require(d.mkdirs())
      }
      d
    }
    listSoFiles(
      rustOutputDir,
      { file =>
        val dest = new File(outputSoDir, file.getName)
        FileUtils.copyFile(file, dest)
        assert(dest.exists())
      }
    )

    FileUtils.copyFile(
      cryptoLibFile,
      new File(outputSoDir, cryptoLibFile.getName)
    )
    FileUtils.copyFile(sslLibFile, new File(outputSoDir, sslLibFile.getName))
  }

  def checkRequiredExtension(extension: RustBuildPluginExtension): Unit = {
    require(extension.getAndroidApi.isPresent)
    require(extension.getTarget.isPresent)
  }

  def getRustProjectDir: File = Option(
    extension.get.getRustProjectDir.getOrNull()
  ) match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir.get, "src/main/jni/rust")
  }

  def getAndroidApi: Int = extension.get.getAndroidApi.get()

  def getArchitecture: String = extension.get.getTarget.get()

  def getTarget: String = s"$getArchitecture-linux-android"

  def getCargoConfigFile: File = new File(getRustProjectDir, ".cargo/config")

  def getAndroidAbi: AndroidAbi = AndroidAbi.from(getTarget)

  def getOpensslDir: File = readOpensslDir

  def getOpensslLibDir: File = new File(getOpensslDir, "lib")

  def getJniLibsDir: File = Option(
    extension.get.getOutputDir.getOrNull()
  ) match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir.get, "jniLibs")
  }

  def getNdkToolchainBinFile: File = ToolchainUtils.getToolchainBinDir(
    new File(extension.get.getAndroidNdkDir.get())
  )

  def getBuildType: BuildType = {
    val buildTypeStr = extension.get.getBuildType.getOrElse("debug")
    BuildType.from(buildTypeStr) match {
      case Some(value) => value
      case None =>
        throw new GradleException(s"Unknown build type: $buildTypeStr")
    }
  }

  def getToolchain: Toolchain =
    new Toolchain(new File(getAndroidNdkDir), getAndroidApi, getArchitecture)

  def getAndroidNdkDir: String = extension.get.getAndroidNdkDir.get()

  def readOpensslDir: File = {
    val propertiesFile = getConfigPropertiesFile
    val notFoundException = new GradleException(
      s"Couldn't find openssl directory. Please define \"openssl-dir\" in ${propertiesFile.getPath}"
    )

    if (!propertiesFile.exists()) {
      throw notFoundException
    }
    val properties = new Properties()
    val is = new FileInputStream(propertiesFile)
    properties.load(is)
    is.close()

    val opensslDir = Option(properties.getProperty("openssl-dir"))
    opensslDir match {
      case Some(dir) =>
        val f = new File(dir)
        if (!f.exists()) {
          throw new GradleException(
            s"\"openssl-dir\" defined in ${propertiesFile.getPath} doesn't exist"
          )
        }
        f
      case None => throw notFoundException
    }
  }

  def getConfigPropertiesFile: File = {
    new File(project.get.getRootProject.getProjectDir, "config.properties")
  }

  class BuildType {
    override def toString: String = this match {
      case BuildType.Debug   => BuildType.Debug.toString
      case BuildType.Release => BuildType.Release.toString
    }
  }

  object BuildType {
    def from(string: String): Option[BuildType] = {
      string match {
        case "debug"   => Some(Debug)
        case "release" => Some(Release)
        case _         => None
      }
    }

    case object Debug extends BuildType {
      override def toString = "debug"
    }

    case object Release extends BuildType {
      override def toString = "release"
    }
  }
}

object RustBuildPlugin {
  trait RustBuildPluginExtension {
    def getOutputDir: Property[String]

    def getRustProjectDir: Property[String]

    def getAndroidNdkDir: Property[String]

    def getAndroidApi: Property[Int]

    // TODO: multi-target handling
    def getTarget: Property[String]

    def getBuildType: Property[String]
  }
}
