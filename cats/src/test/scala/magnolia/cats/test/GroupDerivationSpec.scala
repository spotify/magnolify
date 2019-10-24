package magnolia.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object GroupDerivationSpec extends MagnoliaSpec("GroupDerivation") {
  private def test[T: Arbitrary : ClassTag : Eq : Group]: Unit = include(props[T])

  private def props[T: Arbitrary : ClassTag : Eq : Group]: Properties = {
    ensureSerializable(implicitly[Group[T]])
    new Properties(className[T]) {
      include(GroupTests[T].group.all)
    }
  }

  test[Integers]
}
