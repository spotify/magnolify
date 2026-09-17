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
import org.apache.beam.sdk.schemas.logicaltypes
import org.apache.beam.sdk.schemas.logicaltypes.Timestamp
import org.apache.beam.sdk.values.Row
import org.joda.time as joda
import org.scalacheck.{Arbitrary, Gen, Prop}

import java.nio.ByteBuffer
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import scala.annotation.nowarn
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.*

class RowTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    bst: RowType[T],
    eq: Eq[T]
  ): Unit = testNamed[T](className[T])

  private def testNamed[T: Arbitrary](name: String)(implicit
    bst: RowType[T],
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
    import magnolify.beam.logical.timestamp.millis.*
    testNamed[JavaTime]("JavaTimestampMillis")
    testNamed[JodaTime]("JodaTimestampMillis")
  }

  {
    import magnolify.beam.logical.timestamp.micros.*
    testNamed[JavaTime]("JavaTimestampMicros")
    testNamed[JodaTime]("JodaTimestampMicros")
  }

  {
    import magnolify.beam.logical.timestamp.nanos.*
    testNamed[JavaTime]("JavaTimestampNanos")
    testNamed[JodaTime]("JodaTimestampNanos")
  }

  {
    import magnolify.beam.logical.compat.millis.*
    testNamed[JavaTime]("JavaCompatMillis")
    testNamed[JodaTime]("JodaCompatMillis")
  }

  {
    import magnolify.beam.logical.compat.micros.*
    testNamed[JavaTime]("JavaCompatMicros")
    testNamed[JodaTime]("JodaCompatMicros")
  }

  {
    import magnolify.beam.logical.compat.nanos.*
    testNamed[JavaTime]("JavaCompatNanos")
    testNamed[JodaTime]("JodaCompatNanos")
  }

  // Timestamp#toBaseType throws rather than silently truncating, so these mappings must
  // truncate on write themselves. `subMicro` pins the exact boundary values; `preciseInstants`
  // below generalizes it.
  private val subMicro = Instant.ofEpochSecond(1000L, 123456789L)
  private def instantField(rt: RowType[JavaInstant]): Schema.FieldType =
    rt.schema.getField("i").getType
  private def roundtrip(rt: RowType[JavaInstant]): Instant =
    rt.from(rt.to(JavaInstant(subMicro))).i
  // Timestamp.IDENTIFIER is one shared constant across MILLIS/MICROS/NANOS, so asserting it
  // alone cannot distinguish precisions. getArgument carries the precision.
  private def timestampPrecision(rt: RowType[JavaInstant]): Int = {
    val lt = instantField(rt).getLogicalType
    assertEquals(lt.getIdentifier, Timestamp.IDENTIFIER)
    lt.getArgument[Integer].intValue
  }

  // The shared `arbInstant` generates only millisecond precision and only positive epochs, so the
  // roundtrip properties above hold vacuously: nothing is ever truncated and the epoch boundary is
  // never crossed. These cover both. Note the contract asserted is *not* `roundtrip(i) == i` --
  // each precision discards excess, so the real invariant is truncation to the declared unit.
  // `Instant.truncatedTo` floors, which matters pre-epoch and matches `Timestamp`'s
  // non-negative-subseconds representation.
  private val preciseInstants: Gen[Instant] = for {
    seconds <- Gen.chooseNum(-2208988800L, 4102444800L) // 1900-01-01 .. 2100-01-01
    nanos <- Gen.chooseNum(0, 999999999)
  } yield Instant.ofEpochSecond(seconds, nanos.toLong)

  private def truncatesTo(rt: RowType[JavaInstant], unit: ChronoUnit): Prop =
    Prop.forAll(preciseInstants) { i =>
      rt.from(rt.to(JavaInstant(i))).i == i.truncatedTo(unit)
    }

  {
    import magnolify.beam.logical.timestamp.millis.*
    val rt = RowType[JavaInstant]
    test("timestamp.millis truncates sub-millisecond instants rather than throwing") {
      assertEquals(roundtrip(rt), Instant.ofEpochSecond(1000L, 123000000L))
    }
    test("timestamp.millis maps Instant to Timestamp at precision 3") {
      assertEquals(timestampPrecision(rt), 3)
    }
    property("timestamp.millis truncates to millis across the epoch")(
      truncatesTo(rt, ChronoUnit.MILLIS)
    )
  }

  {
    import magnolify.beam.logical.timestamp.micros.*
    val rt = RowType[JavaInstant]
    test("timestamp.micros truncates sub-microsecond instants rather than throwing") {
      assertEquals(roundtrip(rt), Instant.ofEpochSecond(1000L, 123456000L))
    }
    test("timestamp.micros maps Instant to Timestamp at precision 6") {
      assertEquals(timestampPrecision(rt), 6)
    }
    property("timestamp.micros truncates to micros across the epoch")(
      truncatesTo(rt, ChronoUnit.MICROS)
    )
  }

  {
    import magnolify.beam.logical.timestamp.nanos.*
    val rt = RowType[JavaInstant]
    test("timestamp.nanos preserves full instant precision") {
      assertEquals(roundtrip(rt), subMicro)
    }
    test("timestamp.nanos maps Instant to Timestamp at precision 9") {
      assertEquals(timestampPrecision(rt), 9)
    }
    property("timestamp.nanos preserves nanos across the epoch")(truncatesTo(rt, ChronoUnit.NANOS))
  }

  {
    import magnolify.beam.logical.compat.millis.*
    val rt = RowType[JavaInstant]
    test("compat.millis keeps the joda-backed DATETIME primitive") {
      assertEquals(instantField(rt), Schema.FieldType.DATETIME)
    }
    property("compat.millis truncates to millis across the epoch")(
      truncatesTo(rt, ChronoUnit.MILLIS)
    )
  }

  {
    import magnolify.beam.logical.compat.micros.*
    val rt = RowType[JavaInstant]
    test("compat.micros keeps the raw INT64 encoding") {
      assertEquals(instantField(rt), Schema.FieldType.INT64)
    }
    property("compat.micros truncates to micros across the epoch")(
      truncatesTo(rt, ChronoUnit.MICROS)
    )
  }

  {
    import magnolify.beam.logical.compat.nanos.*
    val rt = RowType[JavaInstant]
    test("compat.nanos keeps the SDK-local NanosInstant logical type") {
      assertEquals(
        instantField(rt).getLogicalType.getIdentifier,
        new logicaltypes.NanosInstant().getIdentifier
      )
    }
    property("compat.nanos preserves nanos across the epoch")(truncatesTo(rt, ChronoUnit.NANOS))
  }

  // The whole point of deprecating rather than repurposing the bare objects: upgrading to 0.10
  // must not silently change the schema of code that still compiles. These pin the bare objects
  // to the 0.9 encodings, field-for-field identical to their `compat` counterparts above.
  {
    @nowarn("cat=deprecation")
    val bareMillis = {
      import magnolify.beam.logical.millis.*
      RowType[JavaInstant]
    }
    @nowarn("cat=deprecation")
    val bareMicros = {
      import magnolify.beam.logical.micros.*
      RowType[JavaInstant]
    }
    @nowarn("cat=deprecation")
    val bareNanos = {
      import magnolify.beam.logical.nanos.*
      RowType[JavaInstant]
    }

    test("deprecated millis still produces the 0.9 DATETIME encoding") {
      assertEquals(instantField(bareMillis), Schema.FieldType.DATETIME)
    }
    test("deprecated micros still produces the 0.9 raw INT64 encoding") {
      assertEquals(instantField(bareMicros), Schema.FieldType.INT64)
    }
    test("deprecated nanos still produces the 0.9 NanosInstant encoding") {
      assertEquals(
        instantField(bareNanos).getLogicalType.getIdentifier,
        new logicaltypes.NanosInstant().getIdentifier
      )
    }

    // Not merely "not a Timestamp" -- assert the schema matches `compat`, so a future edit that
    // touches one grouping and not the other fails here.
    val compatMillis = {
      import magnolify.beam.logical.compat.millis.*
      RowType[JavaInstant]
    }
    val compatMicros = {
      import magnolify.beam.logical.compat.micros.*
      RowType[JavaInstant]
    }
    val compatNanos = {
      import magnolify.beam.logical.compat.nanos.*
      RowType[JavaInstant]
    }

    test("deprecated objects are schema-identical to their compat counterparts") {
      assertEquals(bareMillis.schema, compatMillis.schema)
      assertEquals(bareMicros.schema, compatMicros.schema)
      assertEquals(bareNanos.schema, compatNanos.schema)
    }
  }

  // Documents why `sql` is deprecated: SqlTypes.TIMESTAMP is MicrosInstant, whose
  // toBaseType throws on sub-microsecond precision.
  {
    @nowarn("cat=deprecation")
    val rt = {
      import magnolify.beam.logical.sql.*
      RowType[JavaInstant]
    }
    test("deprecated sql mapping throws on sub-microsecond instants") {
      intercept[AssertionError](rt.to(JavaInstant(subMicro)))
    }
  }

  {
    implicit val bst: RowType[LowerCamel] =
      RowType[LowerCamel](CaseMapper(_.toUpperCase))
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
      RowType[ValueClass]
    }

    implicit val bst: RowType[HasValueClass] = RowType[HasValueClass]
    test[HasValueClass]

    assert(bst.schema.getField("vc").getType == Schema.FieldType.STRING)
    val record = bst(HasValueClass(ValueClass("String")))
    assert(record.getValue[String]("vc").equals("String"))
  }

  {
    @nowarn("cat=deprecation")
    implicit val bst: RowType[Sql] = {
      import magnolify.beam.logical.sql.*
      RowType[Sql]
    }
    test[Sql]
  }

  test("RowType#from handles Row when writer and reader schema fields are in a different order") {
    val rt = RowType[Required]

    // Required case class field order: b (BOOLEAN), i (INT32), s (STRING)
    // Build a Row with fields in a different order: s, i, b
    val reorderedSchema = Schema
      .builder()
      .addField("s", Schema.FieldType.STRING)
      .addField("i", Schema.FieldType.INT32)
      .addField("b", Schema.FieldType.BOOLEAN)
      .build()

    val row = Row
      .withSchema(reorderedSchema)
      .addValues("foo", Int.box(3), Boolean.box(true))
      .build()

    assertEquals(rt.from(row), Required(b = true, i = 3, s = "foo"))
  }

  test(
    "RowType#from handles Row when nested writer and reader schema fields are in a different order"
  ) {
    val rt = RowType[Nested]

    // Build inner Required schema in reverse order: s, i, b
    val innerSchema = Schema
      .builder()
      .addField("s", Schema.FieldType.STRING)
      .addField("i", Schema.FieldType.INT32)
      .addField("b", Schema.FieldType.BOOLEAN)
      .build()

    // Build outer Nested schema in a different order than the case class
    // Case class order: b, i, s, r, o, l
    // Reordered:        r, s, l, b, o, i
    val outerSchema = Schema
      .builder()
      .addField("r", Schema.FieldType.row(innerSchema))
      .addField("s", Schema.FieldType.STRING)
      .addField("l", Schema.FieldType.iterable(Schema.FieldType.row(innerSchema)))
      .addField("b", Schema.FieldType.BOOLEAN)
      .addField("o", Schema.FieldType.row(innerSchema).withNullable(true))
      .addField("i", Schema.FieldType.INT32)
      .build()

    val innerRow = Row
      .withSchema(innerSchema)
      .addValues("inner", Int.box(1), Boolean.box(false))
      .build()

    val row = Row
      .withSchema(outerSchema)
      .addValues(innerRow, "outer", List[Row]().asJava, Boolean.box(true), null, Int.box(2))
      .build()

    val result = rt.from(row)
    assertEquals(result.b, true)
    assertEquals(result.i, 2)
    assertEquals(result.s, "outer")
    assertEquals(result.r, Required(b = false, i = 1, s = "inner"))
    assertEquals(result.o, None)
    assertEquals(result.l, List.empty[Required])
  }
}

case class Empty()
case class Others(bs: ByteString, bb: ByteBuffer, c: Char)
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
case class JavaInstant(i: Instant)
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
