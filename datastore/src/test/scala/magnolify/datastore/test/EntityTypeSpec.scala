/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.datastore.test

import java.net.URI
import java.time.{Duration, Instant}

import cats._
import cats.instances.all._
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.ByteString
import magnolify.datastore._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object EntityTypeSpec extends MagnolifySpec("EntityType") {
  private def test[T: Arbitrary: ClassTag](implicit tpe: EntityType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eq.eqv(t, copy)
    }
  }

  implicit val efInt: EntityField[Int] = EntityField.from[Long](_.toInt)(_.toLong)
  test[Integers]
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
    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
    implicit val efDuration: EntityField[Duration] =
      EntityField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Instant.ofEpochMilli(_)))
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqInstant: Eq[Instant] = Eq.by(_.toEpochMilli)
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    test[DatastoreTypes]
  }

  {
    implicit val efInt: EntityField[Int] =
      EntityField.at[Int](_.getIntegerValue.toInt)(makeValue(_))
    implicit val efUri: EntityField[URI] =
      EntityField.from[String](URI.create)(_.toString)
  }
}

case class DatastoreTypes(bs: ByteString, ts: Instant)
