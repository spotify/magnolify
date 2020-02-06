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
import magnolify.skeleton.proto.TestProto2._
import magnolify.skeleton.proto.TestProtoTypes._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object ProtobufTypeSpec extends MagnolifySpec("ProtobufRecordType") {
  private def test[T: ClassTag : Arbitrary : Eq, U <: Message : ClassTag](
    implicit tpe: ProtobufType[T, U],
    eqMessage: Eq[U] = Eq.instance[U]((first, second) => {
      first.getDescriptorForType == second.getDescriptorForType &&
        (first.getAllFields.asScala == second.getAllFields.asScala)
    })
  ): Unit = {
    ensureSerializable(tpe)

    val eqCaseClass = implicitly[Eq[T]]

    property(className[U]) = Prop.forAll { t: T =>
      val r: U = tpe(t)
      val rCopy: U = r.newBuilderForType().mergeFrom(r).build().asInstanceOf[U]
      val copy: T = tpe(rCopy)
      Prop.all(eqCaseClass.eqv(t, copy), eqMessage.eqv(r, rCopy))
    }
  }

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  test[Required, RequiredP2]
  // we don't support mapping nullable fields into Option, because
  // e.g. Option[Boolean] has three potential values but an optional proto field only has two
  // so in this test the optional fields get mapped to their types, not wrapped in Option
  test[Required, NullableP2]
  test[Required, NullableP3]
  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[NestedNoOption, NestedP2]
  test[NestedNoOption, NestedP3]

  {
    import Collections._
    test[Collections, CollectionP2]
    test[MoreCollections, MoreCollectionP2]
    test[Collections, CollectionP3]
    test[MoreCollections, MoreCollectionP3]
  }

  {
    import Custom._
    implicit val pfUri: ProtobufField[URI] = ProtobufField.from[URI, String](URI.create)(_
      .toString)
    implicit val pfDuration: ProtobufField[Duration] =
      ProtobufField.from[Duration, Long](Duration.ofMillis)(_.toMillis)
    test[Custom, CustomP2]
    test[Custom, CustomP3]
  }
}
