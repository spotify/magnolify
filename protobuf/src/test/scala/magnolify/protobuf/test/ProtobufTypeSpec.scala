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
    eqMessage: Eq[U] = Eq.fromUniversalEquals[U]
  ): Unit = {
//    ensureSerializable(tpe) // TODO not serializable

    property(className[U]) = Prop.forAll { t: T =>
      val r: U = tpe(t)
      val rCopy: U = r.newBuilderForType().mergeFrom(r).build().asInstanceOf[U]
      val copy: T = tpe(rCopy)
      Prop.all(implicitly[Eq[T]].eqv(t, copy), eqMessage.eqv(r, rCopy))
    }
  }

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  test[Required, RequiredP2]
  test[Nullable, NullableP2]
  test[Nullable, NullableP3]
  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[Nested, NestedP2]
  test[Nested, NestedP3]

  // TODO test collections and maps - need to figure out which apply to protobuf
//  {
//    import Collections._
//    test[Collections]
//    test[MoreCollections]
//  }

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