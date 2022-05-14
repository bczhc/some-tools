package pers.zhc.tools.plugin.ndk

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
