/*
 * Copyright 2021 Spotify AB
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

package magnolify.tools

import magnolify.parquet._
import magnolify.test._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, OffsetTime}
import scala.reflect.ClassTag

class ParquetParserSuite extends MagnolifySuite {
  import ParquetParserSuite._

  private def test[T: ClassTag](schema: Record)(implicit t: ParquetType[T]): Unit =
    test[T](null, schema)

  private def test[T: ClassTag](suffix: String, schema: Record)(implicit
    t: ParquetType[T]
  ): Unit = {
    val name = className[T] + (if (suffix == null) "" else "." + suffix)
    test(name) {
      assertEquals(ParquetParser.parse(t.schema), schema)
    }
  }

  private val namespace = Some(this.getClass.getCanonicalName)

  test[Primitives](
    Record(
      Some("Primitives"),
      namespace,
      None,
      List(
        "b" -> Primitive.Boolean,
        "i8" -> Primitive.Byte,
        "i16" -> Primitive.Short,
        "i32" -> Primitive.Int,
        "i64" -> Primitive.Long,
        "f" -> Primitive.Float,
        "d" -> Primitive.Double,
        "ba" -> Primitive.Bytes,
        "s" -> Primitive.String,
        "e" -> Primitive.String
      ).map(kv => Field(kv._1, None, kv._2, Required))
    )
  )

  private val decimalSchema = Record(
    Some("Decimal"),
    namespace,
    None,
    List(Field("bd", None, Primitive.BigDecimal, Required))
  )

  {
    implicit val pfDecimal = ParquetField.decimal32(9)
    test[Decimal]("i32", decimalSchema)
  }

  {
    implicit val pfDecimal = ParquetField.decimal64(18)
    test[Decimal]("i64", decimalSchema)
  }

  {
    implicit val pfDecimal = ParquetField.decimalFixed(8, 18, 0)
    test[Decimal]("fixed", decimalSchema)
  }

  {
    implicit val pfDecimal = ParquetField.decimalBinary(20, 0)
    test[Decimal]("binary", decimalSchema)
  }

  test[Date](
    Record(Some("Date"), namespace, None, List(Field("d", None, Primitive.LocalDate, Required)))
  )

  private val dateTimeSchema = Record(
    Some("DateTime"),
    namespace,
    None,
    List(
      "i" -> Primitive.Instant,
      "dt" -> Primitive.LocalDateTime,
      "ot" -> Primitive.OffsetTime,
      "t" -> Primitive.LocalTime
    ).map(kv => Field(kv._1, None, kv._2, Required))
  )

  {
    import magnolify.parquet.logical.millis._
    test[DateTime]("millis", dateTimeSchema)
  }

  {
    import magnolify.parquet.logical.micros._
    test[DateTime]("micros", dateTimeSchema)
  }

  {
    import magnolify.parquet.logical.nanos._
    test[DateTime]("nanos", dateTimeSchema)
  }

  private def kvSchema(valueSchema: Schema) = Record(
    None,
    None,
    None,
    List(
      Field("key", None, Primitive.String, Required),
      Field("value", None, valueSchema, Required)
    )
  )

  private val repetitionsSchema = Record(
    Some("Repetitions"),
    namespace,
    None,
    List(
      Field("r", None, Primitive.Int, Required),
      Field("o", None, Primitive.Int, Optional),
      Field("l", None, Primitive.Int, Repeated),
      Field("m", None, kvSchema(Primitive.Int), Repeated)
    )
  )

  test[Repetitions](repetitionsSchema)

  {
    import magnolify.parquet.ParquetArray.AvroCompat._
    test[Repetitions]("Avro", repetitionsSchema)
  }

  private val innerSchema =
    Record(None, None, None, List(Field("i", None, Primitive.Int, Required)))
  private val outerSchema = Record(
    Some("Outer"),
    namespace,
    None,
    List(
      Field("r", None, innerSchema, Required),
      Field("o", None, innerSchema, Optional),
      Field("l", None, innerSchema, Repeated),
      Field("m", None, kvSchema(innerSchema), Repeated)
    )
  )

  test[Outer](outerSchema)
}

object ParquetParserSuite {
  object Color extends Enumeration {
    val Red, Green, Blue = Value
  }

  case class Primitives(
    b: Boolean,
    i8: Byte,
    i16: Short,
    i32: Int,
    i64: Long,
    f: Float,
    d: Double,
    ba: Array[Byte],
    s: String,
    e: Color.Value
  )

  case class Decimal(bd: BigDecimal)
  case class Date(d: LocalDate)
  case class DateTime(i: Instant, dt: LocalDateTime, ot: OffsetTime, t: LocalTime)
  case class Repetitions(r: Int, o: Option[Int], l: List[Int], m: Map[String, Int])
  case class Inner(i: Int)
  case class Outer(r: Inner, o: Option[Inner], l: List[Inner], m: Map[String, Inner])
}
