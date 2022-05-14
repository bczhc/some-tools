package pers.zhc.tools.plugin.ndk

case class Target(abi: AndroidAbi, api: Int)

object Target {
  type Targets = List[Target]
}
