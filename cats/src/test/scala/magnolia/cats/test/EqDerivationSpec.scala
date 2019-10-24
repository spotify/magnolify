package magnolia.cats.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object EqDerivationSpec extends MagnoliaSpec("EqDerivation") {
  private def test[T: Arbitrary : ClassTag : Cogen : Eq]: Unit = include(props[T])
  private def test[T: Arbitrary : ClassTag : Cogen : Eq](seed: Long): Unit =
    includeWithSeed(props[T], 0)

  private def props[T: Arbitrary : ClassTag : Cogen : Eq]: Properties = {
    ensureSerializable(implicitly[Eq[T]])
    new Properties(className[T]) {
      include(EqTests[T].eqv.all)
    }
  }

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    test[Collections]
  }

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
    test[Custom]
  }

  test[Node](0)
  test[GNode[Int]](0)
  test[Shape]
  test[Color]
}
