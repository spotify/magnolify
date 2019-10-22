package magnolia.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats._
import magnolia.scalacheck._
import magnolia.test.Simple._
import org.scalacheck._

import scala.reflect._

object GroupDerivationSpec extends Properties("GroupDerivation") {
  // FIXME: fix ambiguous implicit values
  import scala.language.experimental.macros
  implicit def genGroup[T]: Group[T] = macro CatsMacros.genGroupMacro[T]

  private def test[T: Arbitrary : ClassTag : Eq : Group]: Unit = {
    val name = classTag[T].runtimeClass.getSimpleName
    include(GroupTests[T].semigroup.all, s"$name.")
  }

  test[Integers]
}
