package pers.zhc.tools.plugin.ndk

import org.gradle.api.provider.Property

trait NdkBaseExtension {
  def getOutputDir: Property[String]

  def getSrcDir: Property[String]

  def getNdkDir: Property[String]

  def getTargets: Property[Any]

  def getBuildType: Property[String]
}
