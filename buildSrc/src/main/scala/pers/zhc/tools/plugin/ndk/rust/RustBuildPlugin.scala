package pers.zhc.tools.plugin.ndk.rust

import org.gradle.api.provider.Property
import org.gradle.api.{GradleException, Plugin, Project, Task}
import pers.zhc.tools.plugin.ndk.NdkUtils.JMap
import pers.zhc.tools.plugin.ndk.Target.Targets
import pers.zhc.tools.plugin.ndk.rust.BuildRunner.BuildOptions
import pers.zhc.tools.plugin.ndk.rust.RustBuildPlugin._
import pers.zhc.tools.plugin.ndk.{BuildType, NdkBaseExtension, NdkUtils}
import pers.zhc.tools.plugin.util.{FileUtils, ProcessUtils}

import java.io.File
import scala.jdk.CollectionConverters.{MapHasAsScala, SeqHasAsJava}

class RustBuildPlugin extends Plugin[Project] {
  var appProjectDir: File = _
  var config: Configurations = _
  var project: Project = _

  override def apply(project: Project): Unit = {
    val extension = project.getExtensions
      .create("rustBuild", classOf[RustBuildPluginExtension])
    val setConfigs = { () =>
      this.config = getConfigurations(extension)
    }

    val registerTask = { (name: String, taskAction: Task => Unit) =>
      project.task(
        name,
        { task: Task =>
          task.doFirst { _: Task => setConfigs() }
          task.doLast { task: Task => taskAction(task) }
          ()
        }
      )
    }

    registerTask(
      TASK_NAME,
      { _ =>
        compileRust()
      }
    )

    registerTask(
      TASK_CLEAN_NAME,
      { _ =>
        cleanRust()
      }
    )

    val appProject = project.getRootProject.findProject("app")
    require(appProject != null)
    this.appProjectDir = appProject.getProjectDir
    this.project = project
  }

  def cleanRust(): Unit = {
    val command = List("cargo", "clean")
    val pb = new ProcessBuilder(command.asJava)
      .directory(config.rustProjectDir)
    val status = ProcessUtils.executeWithOutput(pb.start())
    if (status != 0) {
      throw new GradleException(
        s"Failed to clean rust project: non-zero exit code: $status"
      )
    }
  }

  def compileRust(): Unit = {
    val jniLibsDir = getJniLibsDir

    for (target <- config.targets) {
      println(s"Build target: $target")
      val toolchain =
        new Toolchain(config.androidNdkDir, target.api, target.abi)
      val status = new BuildRunner(
        toolchain,
        BuildOptions(
          target,
          config.buildType,
          config.extraEnv,
          config.rustProjectDir
        )
      ).run()
      if (status != 0) {
        throw new GradleException(
          "Failed to run program: exits with non-zero return value"
        )
      }

      val rustOutputDir =
        new File(
          config.rustProjectDir,
          s"target/${target.abi.toRustTarget}/${config.buildType.toString}"
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
        val d = new File(jniLibsDir, target.abi.toString)
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
          println(s"Output: ${dest.getPath}")
          assert(dest.exists())
        }
      )
    }
  }

  def getJniLibsDir: File = config.outputDir match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir, "jniLibs")
  }

  def getConfigurations(extension: RustBuildPluginExtension): Configurations = {
    def toOption[T](value: Property[T]) = Option(value.getOrNull())

    def unwrap[T](p: Property[T], name: String): T = {
      NdkUtils.unwrapProperty(p, name)
    }

    new Configurations {
      override val outputDir: Option[String] = toOption(extension.getOutputDir)

      override val rustProjectDir: File =
        toOption(extension.getSrcDir).map(new File(_)) match {
          case Some(value) => value
          case None =>
            new File(appProjectDir, "src/main/jni/rust")
        }

      override val androidNdkDir: File = new File(
        unwrap(extension.getNdkDir, "androidNdkDir")
      )

      override val targets: Targets =
        NdkUtils.propertyToTargets(extension.getTargets)

      override val buildType: BuildType =
        BuildType.from(toOption(extension.getBuildType).getOrElse("debug"))

      override val extraEnv: Option[Map[String, String]] =
        toOption(extension.getExtraEnv).map({
          _.asInstanceOf[JMap[String, String]].asScala.toMap
        })
    }
  }
}

object RustBuildPlugin {
  val TASK_NAME = "compileRust"
  val TASK_CLEAN_NAME = "cleanRust"

  trait RustBuildPluginExtension extends NdkBaseExtension {
    def getExtraEnv: Property[Any]
  }

  abstract class Configurations {
    val outputDir: Option[String]
    val rustProjectDir: File
    val androidNdkDir: File
    val targets: Targets
    val buildType: BuildType
    val extraEnv: Option[Map[String, String]]
  }

  type Environments = Map[String, String]
}
