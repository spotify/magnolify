package magnolia.scalacheck

import org.scalacheck._

import scala.language.experimental.macros
import scala.reflect.macros._

package object auto {
  def genArbitraryMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.scalacheck.semiauto.ArbitraryDerivation.apply[$wtt]"""
  }

  def genCogenMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.scalacheck.semiauto.CogenDerivation.apply[$wtt]"""
  }

  implicit def genArbitrary[T]: Arbitrary[T] = macro genArbitraryMacro[T]
  implicit def genCogen[T]: Cogen[T] = macro genCogenMacro[T]
}
