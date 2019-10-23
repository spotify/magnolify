package magnolia.cats.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.SerializableUtils
import magnolia.test.Simple._
import org.scalacheck._

import scala.reflect._

object EqDerivationSpec extends Properties("EqDerivation") {
  private def test[T: Arbitrary : ClassTag : Cogen : Eq]: Unit = {
    SerializableUtils.ensureSerializable(implicitly[Eq[T]])
    val name = classTag[T].runtimeClass.getSimpleName
    include(EqTests[T].eqv.all, s"$name.")
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

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}
