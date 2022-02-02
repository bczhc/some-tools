package pers.zhc.tools.plugin.rust

import org.gradle.api.provider.Property
import org.gradle.api.{GradleException, Plugin, Project, Task}
import pers.zhc.tools.plugin.rust.RustBuildPlugin.{
  BuildType,
  Configurations,
  RustBuildPluginExtension,
  TASK_NAME
}
import pers.zhc.tools.plugin.util.FileUtils

import java.io.File
import scala.jdk.CollectionConverters.MapHasAsScala

class RustBuildPlugin extends Plugin[Project] {
  var appProjectDir: File = _
  var config: Configurations = _
  var project: Project = _

  override def apply(project: Project): Unit = {
    project.task(
      TASK_NAME,
      { task: Task =>
        val extension = project.getExtensions
          .create("rustBuild", classOf[RustBuildPluginExtension])
        task.doFirst { _: Task => this.config = getConfigurations(extension) }
        task.doLast { _: Task =>
          compileReleaseRust()
        }
        ()
      }
    )

    val appProject = project.getRootProject.findProject("app")
    require(appProject != null)
    this.appProjectDir = appProject.getProjectDir
    this.project = project
  }

  def compileReleaseRust(): Unit = {
    val jniLibsDir = getJniLibsDir

    val toolchain = getToolchain
    val status = new BuildRunner(toolchain, config).run()
    if (status != 0) {
      throw new GradleException(
        "Failed to run program: exits with non-zero return value"
      )
    }

    val rustOutputDir =
      new File(
        config.rustProjectDir,
        s"target/${config.targetAbi.toRustTarget}/${config.buildType.toString}"
      )
    assert(rustOutputDir.exists())

    if (!jniLibsDir.exists()) {
      require(jniLibsDir.mkdir())
    }

    val listSoFiles: (File, File => Unit) => Unit = { (dir, func) =>
      dir.listFiles().foreach { f =>
        if (f.isFile && FileUtils.getFileExtensionName(f) == Option("so")) {
          func(f)
        }
      }
    }

    val outputSoDir = {
      val d = new File(jniLibsDir, config.targetAbi.toString)
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
  }

  def getJniLibsDir: File = config.outputDir match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir, "jniLibs")
  }

  def getToolchain: Toolchain =
    new Toolchain(config.androidNdkDir, config.androidApi, config.targetAbi)

  def getConfigurations(extension: RustBuildPluginExtension): Configurations = {
    def toOption[T](value: Property[T]) = Option(value.getOrNull())

    def unwrap[T](p: Property[T], name: String): T = {
      require(
        p.isPresent,
        s"Configuration field \"$name\" is missing"
      )
      p.get()
    }

    new Configurations {
      override val outputDir: Option[String] = toOption(extension.getOutputDir)

      override val rustProjectDir: File =
        toOption(extension.getRustProjectDir).map(new File(_)) match {
          case Some(value) => value
          case None =>
            new File(appProjectDir, "src/main/jni/rust")
        }

      override val androidNdkDir: File = new File(
        unwrap(extension.getAndroidNdkDir, "androidNdkDir")
      )

      override val androidApi: Int =
        unwrap(extension.getAndroidApi, "androidApi")

      override val targetAbi: AndroidAbi =
        AndroidAbi.from(unwrap(extension.getTargetAbi, "targetAbi"))

      override val buildType: BuildType =
        BuildType.from(toOption(extension.getBuildType).getOrElse("debug"))

      override val extraEnv: Option[Map[String, String]] =
        toOption(extension.getExtraEnv).map({
          _.asInstanceOf[java.util.Map[String, String]].asScala.toMap
        })
    }
  }
}

object RustBuildPlugin {
  val TASK_NAME = "compileReleaseRust"
  trait RustBuildPluginExtension {
    def getOutputDir: Property[String]

    def getRustProjectDir: Property[String]

    def getAndroidNdkDir: Property[String]

    def getAndroidApi: Property[Int]

    // TODO: multi-target handling
    // options: armeabi-v7a, arm64-v8a, x86, x86_64
    def getTargetAbi: Property[String]

    def getBuildType: Property[String]

    def getExtraEnv: Property[Object]
  }

  abstract class Configurations {
    val outputDir: Option[String]
    val rustProjectDir: File
    val androidNdkDir: File
    val androidApi: Int
    val targetAbi: AndroidAbi
    val buildType: BuildType
    val extraEnv: Option[Map[String, String]]
  }

  class BuildType {
    override def toString: String = this match {
      case BuildType.Debug   => BuildType.Debug.toString
      case BuildType.Release => BuildType.Release.toString
    }
  }

  object BuildType {
    def from(string: String): BuildType = {
      string match {
        case "debug"   => Debug
        case "release" => Release
        case _         => throw new NoSuchElementException("Invalid build type")
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
