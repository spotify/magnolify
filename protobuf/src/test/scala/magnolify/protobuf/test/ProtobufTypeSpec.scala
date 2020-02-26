/*
 * Copyright 2020 Spotify AB.
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
package magnolify.protobuf.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.Message
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.protobuf._
import magnolify.shims.JavaConverters._
import magnolify.test.Proto2._
import magnolify.test.Proto3._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object ProtobufTypeSpec extends MagnolifySpec("ProtobufRecordType") {
  private def test[T: ClassTag: Arbitrary: Eq, U <: Message: ClassTag](
    implicit tpe: ProtobufType[T, U],
    eqt: Eq[T],
    eqm: Eq[U] = Eq.instance[U](_.toByteString == _.toByteString)
  ): Unit = {
    ensureSerializable(tpe)

    property(className[U]) = Prop.forAll { t: T =>
      val r: U = tpe(t)
      val rCopy: U = r.newBuilderForType().mergeFrom(r).build().asInstanceOf[U]
      val copy: T = tpe(rCopy)
      Prop.all(eqt.eqv(t, copy), eqm.eqv(r, rCopy))
    }
  }

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  // PROTO3 removes the notion of require vs optional fields.
  // The new singular field returns default value if unset, making it required essentially.
  test[Required, RequiredP2]
  test[Required, SingularP3]
  test[Nullable, NullableP2]
  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[NestedNoOption, NestedP2]
  test[NestedNoOption, NestedP3]

  {
    implicit val eqB: Eq[Array[Byte]] = Eq.by(_.toList)
    test[Bytes, BytesP2]
    test[Bytes, BytesP3]
  }

  {
    import Collections._
    test[Collections, CollectionP2]
    test[MoreCollections, MoreCollectionP2]
    test[Collections, CollectionP3]
    test[MoreCollections, MoreCollectionP3]
  }

  {
    import Custom._
    implicit val pfUri: ProtobufField[URI] = ProtobufField.from[String](URI.create)(_.toString)
    implicit val pfDuration: ProtobufField[Duration] =
      ProtobufField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom, CustomP2]
    test[Custom, CustomP3]
  }
}
