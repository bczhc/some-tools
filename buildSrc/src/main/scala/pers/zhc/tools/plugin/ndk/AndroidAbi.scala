package pers.zhc.tools.plugin.ndk

/** @author
  *   bczhc
  */

class AndroidAbi {
  override def toString: String = this match {
    case AndroidAbi.Arm    => "armeabi-v7a"
    case AndroidAbi.Arm64  => "arm64-v8a"
    case AndroidAbi.X86    => "x86"
    case AndroidAbi.X86_64 => "x86_64"
  }

  def toRustTarget: String = this match {
    case AndroidAbi.Arm    => "armv7-linux-androideabi"
    case AndroidAbi.Arm64  => "aarch64-linux-android"
    case AndroidAbi.X86    => "i686-linux-android"
    case AndroidAbi.X86_64 => "x86_64-linux-android"
  }

  def toNdkToolchainName: String = this match {
    case AndroidAbi.Arm    => "armv7a-linux-androideabi"
    case AndroidAbi.Arm64  => "aarch64-linux-android"
    case AndroidAbi.X86    => "i686-linux-android"
    case AndroidAbi.X86_64 => "x86_64-linux-android"
  }
}

case object AndroidAbi {
  def from(targetName: String): AndroidAbi =
    targetName match {
      case "x86_64"      => AndroidAbi.X86_64
      case "x86"         => AndroidAbi.X86
      case "arm64-v8a"   => AndroidAbi.Arm64
      case "armeabi-v7a" => AndroidAbi.Arm
      case _             => throw new NoSuchElementException("Invalid ABI name")
    }

  def tryFrom(targetName: String): Option[AndroidAbi] =
    try {
      Some(from(targetName))
    } catch {
      case _: NoSuchElementException => None
    }

  case object Arm extends AndroidAbi {}

  case object Arm64 extends AndroidAbi {}

  case object X86 extends AndroidAbi {}

  case object X86_64 extends AndroidAbi {}
}
