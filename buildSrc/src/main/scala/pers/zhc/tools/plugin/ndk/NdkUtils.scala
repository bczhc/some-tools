package pers.zhc.tools.plugin.ndk

import org.gradle.api.provider.Property

import scala.jdk.CollectionConverters.CollectionHasAsScala

object NdkUtils {
  type JMap[K, V] = java.util.Map[K, V]
  type JList[T] = java.util.List[T]

  def propertyToTargets(targets: Property[Any]): Target.Targets = {
    unwrapProperty(targets, "targets")
      .asInstanceOf[JList[JMap[String, Any]]]
      .asScala
      .toList
      .map { it =>
        Target(
          AndroidAbi.from(it.get("abi").asInstanceOf[String]),
          it.get("api").asInstanceOf[Int]
        )
      }
  }

  def unwrapProperty[T](p: Property[T], name: String): T = {
    require(
      p.isPresent,
      s"Configuration field \"$name\" is missing"
    )
    p.get()
  }
}
