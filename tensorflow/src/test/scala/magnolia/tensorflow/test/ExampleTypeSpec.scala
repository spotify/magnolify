package magnolia.tensorflow.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.ByteString
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.tensorflow._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object ExampleTypeSpec extends MagnoliaSpec("ExampleType") {
  private def test[T: Arbitrary : ClassTag](implicit tpe: ExampleType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eq.eqv(t, copy)
    }
  }

  implicit val efInt: ExampleField[Int] = ExampleField.atLong(_.toInt)(_.toLong)

  {
    implicit val efBoolean: ExampleField[Boolean] =
      ExampleField.atLong(_ == 1)(x => if (x) 1 else 0)
    implicit val efString: ExampleField[String] =
      ExampleField.atBytes(_.toStringUtf8)(ByteString.copyFromUtf8)
    test[Integers]
    test[Required]
    test[Nullable]
    test[Repeated]
    // FIXME: flatten nested types
    // test[Nested]
  }

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    test[Collections]
  }

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
    implicit val efUri: ExampleField[URI] = ExampleField.atBytes(
      x => URI.create(x.toStringUtf8))(x => ByteString.copyFromUtf8(x.toString))
    implicit val efDuration: ExampleField[Duration] =
      ExampleField.atLong(Duration.ofMillis)(_.toMillis)

    test[Custom]
  }
}
