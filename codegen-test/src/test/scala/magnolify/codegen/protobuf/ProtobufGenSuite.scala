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
package magnolify.codegen.protobuf

import java.net.URI

import com.google.protobuf.{ByteString, Message}
import magnolify.codegen._
import magnolify.protobuf._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.test._
import org.scalacheck._

import scala.reflect.ClassTag

class ProtobufGenSuite extends MagnolifySuite {
  private def test[T: ClassTag, M <: Message: ClassTag](implicit
    pt: ProtobufType[T, M],
    arb: Arbitrary[T]
  ): Unit =
    property(s"${className[T]}.${className[M]}") {
      Prop.forAll { t: T =>
        assertEquals(pt(pt(t)), t)
      }
    }

  test[NumbersP2, Proto2.NumbersP2]
  test[RequiredP2, Proto2.RequiredP2]
  test[NullableP2, Proto2.NullableP2]
  test[RepeatedP2, Proto2.RepeatedP2]
  test[NestedP2, Proto2.NestedP2]

  test[NumbersP3, Proto3.NumbersP3]
  test[SingularP3, Proto3.SingularP3]
  test[RepeatedP3, Proto3.RepeatedP3]
  test[NestedP3, Proto3.NestedP3]

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    test[BytesP3, Proto3.BytesP3]
  }

  {
    implicit val pt: ProtobufType[UpperCase, Proto3.UpperCase] =
      ProtobufType[UpperCase, Proto3.UpperCase](CaseMapper(_.toUpperCase))
    test[UpperCase, Proto3.UpperCase]
  }

  {
    import magnolify.protobuf.unsafe.Proto3Option._
    implicit val pt: ProtobufType[proto3option.SingularP3, Proto3.SingularP3] =
      ProtobufType[proto3option.SingularP3, Proto3.SingularP3]
    implicit def arbOption[T](implicit arb: Arbitrary[T]): Arbitrary[Option[T]] =
      Arbitrary(arb.arbitrary.map(Some(_)))
    test[proto3option.SingularP3, Proto3.SingularP3]
  }

  test[repeated.RepeatedP3, Proto3.RepeatedP3]

  {
    implicit val arbUri: Arbitrary[URI] = Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val pfUri: ProtobufField[URI] = ProtobufField.from[String](URI.create)(_.toString)
    test[overrides.SingularP3, Proto3.SingularP3]
  }
}
