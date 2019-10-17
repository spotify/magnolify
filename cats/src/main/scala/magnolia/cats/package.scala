package magnolia

import _root_.cats._

import scala.language.experimental.macros
import scala.reflect.macros._

package object cats {
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.EqDerivation.gen[$wtt]"""
  }

  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.SemigroupDerivation.gen[$wtt]"""
  }

  implicit def genEq[T]: Eq[T] = macro genEqMacro[T]
  implicit def genSemigroup[T]: Semigroup[T] = macro genSemigroupMacro[T]
}
