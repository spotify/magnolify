package magnolify.scalacheck

import org.scalacheck.{Arbitrary, Cogen}

import scala.reflect.macros.*
object ScalaCheckMacros {
  def genArbitraryMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.scalacheck.ArbitraryDerivation.gen[$wtt]"""
  }

  def genCogenMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.scalacheck.CogenDerivation.gen[$wtt]"""
  }

}

trait AutoDerivations {
  implicit def genArbitrary[T]: Arbitrary[T] = macro ScalaCheckMacros.genArbitraryMacro[T]
  implicit def genCogen[T]: Cogen[T] = macro ScalaCheckMacros.genCogenMacro[T]
}
