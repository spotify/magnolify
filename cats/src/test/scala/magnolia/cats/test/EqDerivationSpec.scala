package magnolia.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import com.google.protobuf.ByteString
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.SerializableUtils
import magnolia.test.Simple._
import org.joda.time.Duration
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

  import Custom._
  implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
  implicit val eqDuration: Eq[Duration] = Eq.by(_.getMillis)
  test[Custom]

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}
