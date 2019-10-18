package magnolia

import org.scalacheck.Arbitrary

import scala.language.experimental.macros
import scala.reflect.macros._

package object scalacheck {
  def genArbitraryMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.scalacheck.ArbitraryDerivation.gen[$wtt]"""
  }

  def genArbitraryFnMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.scalacheck.ArbitraryFnDerivation.gen[$wtt]"""
  }

  implicit def genArbitrary[T]: Arbitrary[T] = macro genArbitraryMacro[T]
  implicit def genArbitraryFn[T]: Arbitrary[T => T] = macro genArbitraryFnMacro[T]
}
