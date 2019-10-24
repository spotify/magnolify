package magnolia.cats.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object MonoidDerivationSpec extends MagnoliaSpec("MonoidDerivation") {
  private def test[T: Arbitrary : ClassTag : Eq : Monoid]: Unit = include(props[T])

  private def props[T: Arbitrary : ClassTag : Eq : Monoid]: Properties = {
    ensureSerializable(implicitly[Monoid[T]])
    new Properties(className[T]) {
      include(MonoidTests[T].monoid.all)
    }
  }

  test[Integers]

  {
    implicit val mBool: Monoid[Boolean] = Monoid.instance(false, _ || _)
    test[Required]
    test[Nullable]
    test[Repeated]
    test[Nested]
  }

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    implicit val mArray: Monoid[Array[Int]] = Monoid.instance(Array.emptyIntArray, _ ++ _)
    test[Collections]
  }

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
    implicit val mUri: Monoid[URI] =
      Monoid.instance(URI.create(""), (x, y) => URI.create(x.toString + y.toString))
    implicit val mDuration: Monoid[Duration] = Monoid.instance(Duration.ZERO, _ plus _)
    test[Custom]
  }
}
