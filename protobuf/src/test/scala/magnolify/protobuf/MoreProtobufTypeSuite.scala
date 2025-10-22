/*
 * Copyright 2025 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnolify.protobuf

import java.net.URI
import java.time.Duration
import cats.*
import com.google.protobuf.ByteString
import magnolify.cats.auto.*
import magnolify.scalacheck.auto.*
import magnolify.protobuf.*
import magnolify.protobuf.Proto2.*
import magnolify.protobuf.Proto3.*
import magnolify.protobuf.unsafe.*
import magnolify.shared.*
import magnolify.test.Simple.*
import org.scalacheck.*
import magnolify.scalacheck.TestArbitrary.*
import magnolify.cats.TestEq.{eqEnums as _, eqNullable as _, *}

// Workaround for "Method too large: magnolify/protobuf/test/ProtobufTypeSuite.<init> ()V"
class MoreProtobufTypeSuite extends BaseProtobufTypeSuite {

  implicit val arbByteString: Arbitrary[ByteString] =
    Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
  implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
  implicit val pfUri: ProtobufField[URI] = ProtobufField.from[String](URI.create)(_.toString)
  implicit val pfDuration: ProtobufField[Duration] =
    ProtobufField.from[Long](Duration.ofMillis)(_.toMillis)

  test[BytesA, BytesP2]
  test[BytesB, BytesP3]

  {
    import Proto2Enums._
    test[Enums, EnumsP2]
    test[UnsafeEnums, UnsafeEnumsP2]
  }

  {
    import Proto3Enums._
    test[Enums, EnumsP3]
    test[UnsafeEnums, UnsafeEnumsP3]
  }

  test[Custom, CustomP2]
  test[Custom, CustomP3]

  {
    implicit val pt: ProtobufType[LowerCamel, UpperCaseP3] =
      ProtobufType[LowerCamel, UpperCaseP3](CaseMapper(_.toUpperCase))
    test[LowerCamel, UpperCaseP3]
  }

  {
    import Proto2Enums._
    test[DefaultsRequired2, DefaultRequiredP2]
    test[DefaultsNullable2, DefaultNullableP2]
  }

  {
    import Proto3Enums._
    test[DefaultIntegers3, IntegersP3]
    test[DefaultFloats3, FloatsP3]
    test[DefaultRequired3, RequiredP3]
    test[DefaultEnums3, EnumsP3]
  }

  {
    test[DefaultNullable3, NullableP3]
  }

  {
    import Proto2Enums._
    type F[T] = ProtobufType[T, _]
    testFail[F, DefaultMismatch2](ProtobufType[DefaultMismatch2, DefaultRequiredP2])(
      "Default mismatch magnolify.protobuf.DefaultMismatch2#i: 321 != 123"
    )
    testFail[F, DefaultMismatch3](ProtobufType[DefaultMismatch3, RequiredP3])(
      "Default mismatch magnolify.protobuf.DefaultMismatch3#i: 321 != 0"
    )
  }

  {

    /**
     * Issue #1001: Re-use of the protobuf builder for iterables (Seq[Bug1001Nested]), where the
     * items in the iterable had repeated or optional fields (Seq[Bug1001Metadata]) resulted in the
     * values from previous items (Bug1001Nested) being carried over to subsequent ones where those
     * fields were empty in the subsequent item.
     */
    import magnolify.protobuf.Bug1001._
    test[Bug1001Scala, Bug1001Pb]
  }
}

final case class Bug1001Metadata(key: String, value: String)
final case class Bug1001Nested(id: String, metadata: Seq[Bug1001Metadata])
final case class Bug1001Scala(entities: Seq[Bug1001Nested])
