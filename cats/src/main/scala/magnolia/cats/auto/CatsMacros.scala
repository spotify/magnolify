package magnolia.cats.auto

import _root_.cats._
import cats.kernel.instances.{ListMonoid, OptionMonoid}

import scala.language.experimental.macros
import scala.reflect.macros._

private object CatsMacros {
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.semiauto.EqDerivation.apply[$wtt]"""
  }

  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.semiauto.SemigroupDerivation.apply[$wtt]"""
  }

  def genMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.semiauto.MonoidDerivation.apply[$wtt]"""
  }

  def genGroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolia.cats.semiauto.GroupDerivation.apply[$wtt]"""
  }
}

trait LowPriorityGenSemigroup {
  implicit def genSemigroup[T]: Semigroup[T] = macro CatsMacros.genSemigroupMacro[T]
}

trait LowPriorityGenMonoid extends LowPriorityGenSemigroup {
  implicit def genMonoid[T]: Monoid[T] = macro CatsMacros.genMonoidMacro[T]
}

trait LowPriorityGenGroup extends LowPriorityGenMonoid {
  implicit def genGroup[T]: Group[T] = macro CatsMacros.genGroupMacro[T]
}

trait LowPriorityImplicits extends LowPriorityGenGroup {
  implicit def genEq[T]: Eq[T] = macro CatsMacros.genEqMacro[T]

  // workaround for ambiguous implicit values with cats
  implicit def genListMonoid[T] = new ListMonoid[T]
  implicit def genOptionMonoid[T: Semigroup] = new OptionMonoid[T]
}
