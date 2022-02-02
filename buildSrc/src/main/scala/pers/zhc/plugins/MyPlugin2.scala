package pers.zhc.plugins

import org.gradle.api.{Plugin, Project}

/** @author
  *   bczhc
  */
class MyPlugin2 extends Plugin[Project] {
  override def apply(t: Project): Unit = {
    println("Hello, Scala!")
  }
}
