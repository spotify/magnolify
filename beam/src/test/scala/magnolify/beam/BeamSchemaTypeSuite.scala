/*
 * Copyright 2024 Spotify AB
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

package magnolify.beam

import cats.*
import com.google.protobuf.ByteString
import magnolify.cats.auto.*
import magnolify.cats.TestEq.*
import magnolify.scalacheck.auto.*
import magnolify.scalacheck.TestArbitrary.*
import magnolify.shared.CaseMapper
import magnolify.test.ADT
import magnolify.test.MagnolifySuite
import magnolify.test.Simple.*
import org.apache.beam.sdk.schemas.Schema
import org.joda.time as joda
import org.scalacheck.{Arbitrary, Gen, Prop}

import java.nio.ByteBuffer
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.*

class BeamSchemaTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    bst: BeamSchemaType[T],
    eq: Eq[T]
  ): Unit = testNamed[T](className[T])

  private def testNamed[T: Arbitrary](name: String)(implicit
    bst: BeamSchemaType[T],
    eq: Eq[T]
  ): Unit = {
    // Ensure serializable even after evaluation of `schema`
    bst.schema: Unit
    ensureSerializable(bst)

    property(name) {
      Prop.forAll { (t: T) =>
        val converted = bst.apply(t)
        val roundtripped = bst.apply(converted)
        Prop.all(eq.eqv(t, roundtripped))
      }
    }
  }

  implicit val arbByteString: Arbitrary[ByteString] =
    Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
  implicit val arbBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(BigDecimal(_)))
  implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Collections]
  test[MoreCollections]

  test[Empty]
  test[Others]
  test[Maps]
  test[Logical]
  test[Decimal]

  {
    import magnolify.shared.TestEnumType._
    test[SealedTest]
  }

  {
    import magnolify.beam.unsafe._
    import magnolify.shared.TestEnumType._
    test[Enums]
    test[UnsafeEnums]
  }

  {
    import magnolify.beam.logical.date.*
    test[JavaDate]
    test[JodaDate]
  }

  {
    import magnolify.beam.logical.millis.*
    testNamed[JavaTime]("JavaMillis")
    testNamed[JodaTime]("JodaMillis")
  }

  {
    import magnolify.beam.logical.micros.*
    testNamed[JavaTime]("JavaMicros")
    testNamed[JodaTime]("JodaMicros")
  }

  {
    import magnolify.beam.logical.nanos.*
    testNamed[JavaTime]("JavaNanos")
    testNamed[JodaTime]("JodaNanos")
  }

  {
    implicit val bst: BeamSchemaType[LowerCamel] =
      BeamSchemaType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    {
      val schema = bst.schema
      val fields = LowerCamel.fields.map(_.toUpperCase)
      assertEquals(schema.getFields.asScala.map(_.getName()).toSeq, fields)
      assertEquals(
        schema.getField("INNERFIELD").getType.getRowSchema.getFields.asScala.map(_.getName()).toSeq,
        Seq("INNERFIRST")
      )
    }
  }

  {
    // value classes should act only as fields
    intercept[IllegalArgumentException] {
      BeamSchemaType[ValueClass]
    }

    implicit val bst: BeamSchemaType[HasValueClass] = BeamSchemaType[HasValueClass]
    test[HasValueClass]

    assert(bst.schema.getField("vc").getType == Schema.FieldType.STRING)
    val record = bst(HasValueClass(ValueClass("String")))
    assert(record.getValue[String]("vc").equals("String"))
  }

  {
    import magnolify.beam.logical.sql.*
    test[Sql]
  }
}

case class Empty()
case class Others(bs: ByteString, cs: CharSequence, bb: ByteBuffer, c: Char)
case class Decimal(bd: BigDecimal, bdo: Option[BigDecimal])
case class Logical(
  u: UUID,
  uo: Option[UUID],
  ul: List[UUID],
  ulo: List[Option[UUID]]
)

case class Sql(
  i: Instant,
  dt: LocalDateTime,
  t: LocalTime,
  d: LocalDate
)
case class JavaDate(d: LocalDate)
case class JodaDate(jd: joda.LocalDate)
case class JavaTime(
  i: Instant,
  dt: LocalDateTime,
  t: LocalTime,
  d: Duration
)
case class JodaTime(
  i: joda.Instant,
  dt: joda.DateTime,
  lt: joda.LocalTime,
  d: joda.Duration,
  ldt: joda.LocalDateTime
)
case class Maps(
  ms: Map[String, String],
  mi: Map[Int, Int],
  ml: Map[Long, Long],
  md: Map[Double, Double],
  mf: Map[Float, Float],
  mb: Map[Byte, Byte],
  msh: Map[Short, Short],
  mba: Map[Byte, Array[Byte]],
  mbs: Map[ByteString, Array[Byte]],
  mso: Map[Option[String], Option[String]],
  mu: Map[UUID, UUID],
  mlo: Map[Option[UUID], Option[UUID]]
)

case class SealedTest(shape: ADT.Shape, point: ADT.Rect, enumColor: ADT.Color)
