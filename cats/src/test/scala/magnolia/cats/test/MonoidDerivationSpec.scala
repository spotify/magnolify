package magnolia.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import com.google.protobuf.ByteString
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.SerializableUtils
import magnolia.test.Simple._
import org.joda.time.Duration
import org.scalacheck._

import scala.reflect._

object MonoidDerivationSpec extends Properties("MonoidDerivation") {
  private def test[T: Arbitrary : ClassTag : Eq : Monoid]: Unit = {
    SerializableUtils.ensureSerializable(implicitly[Monoid[T]])
    val name = classTag[T].runtimeClass.getSimpleName
    include(MonoidTests[T].semigroup.all, s"$name.")
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
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.getMillis)
    implicit val mByteString: Monoid[ByteString] = Monoid.instance(ByteString.EMPTY, _ concat _)
    implicit val mDuration: Monoid[Duration] = Monoid.instance(Duration.ZERO, _ plus _)
    test[Custom]
  }

}
