package pers.zhc.tools.plugin.rust

/**
 * @author bczhc
 */

class AndroidAbi {
  override def toString: String = this match {
    case AndroidAbi.Arm => "armeabi-v7a"
    case AndroidAbi.Arm64 => "arm64-v8a"
    case AndroidAbi.X86 => "x86"
    case AndroidAbi.X86_64 => "x86_64"
  }
}

object AndroidAbi {
  case object Arm extends AndroidAbi {}

  case object Arm64 extends AndroidAbi {}

  case object X86 extends AndroidAbi {}

  case object X86_64 extends AndroidAbi {}

  def from(targetName: String): AndroidAbi = targetName match {
    case "x86_64-linux-android" => AndroidAbi.X86_64
    case "i686-linux-android" => AndroidAbi.X86
    case "aarch64-linux-android" => AndroidAbi.Arm64
    case "armv7-linux-androideabi" => AndroidAbi.Arm
  }
}