package magnolia.datastore.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import magnolia.datastore._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.SerializableUtils
import magnolia.test.Simple._
import org.scalacheck._

import scala.reflect._

object EntityTypeSpec extends Properties("EntityType") {
  private def test[T: Arbitrary : Eq : ClassTag](implicit tpe: EntityType[T]): Unit = {
    SerializableUtils.ensureSerializable(tpe)
    val name = classTag[T].runtimeClass.getSimpleName
    val eq = implicitly[Eq[T]]
    property(s"$name") = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eq.eqv(t, copy)
    }
  }

  implicit val efInt = EntityField.at[Int](_.getIntegerValue.toInt)(makeValue(_))
  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
    implicit val efUri: EntityField[URI] = EntityField[String].imap(URI.create)(_.toString)
    implicit val efDuration: EntityField[Duration] =
      EntityField[Long].imap(Duration.ofMillis)(_.toMillis)
    test[Custom]
  }
}
