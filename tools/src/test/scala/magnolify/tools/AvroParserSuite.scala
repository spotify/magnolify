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

import magnolify.avro._
import magnolify.test._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import scala.reflect.ClassTag

class AvroParserSuite extends MagnolifySuite {
  import AvroParserSuite._

  private def test[T: ClassTag](schema: Record)(implicit t: AvroType[T]): Unit =
    test[T](null, schema)

  private def test[T: ClassTag](suffix: String, schema: Record)(implicit t: AvroType[T]): Unit = {
    val name = className[T] + (if (suffix == null) "" else "." + suffix)
    test(name) {
      assertEquals(AvroParser.parse(t.schema), schema)
    }
  }

  test[Primitives](
    Record(
      Some("Primitives"),
      None,
      List(
        Record.Field("b", None, Primitive.Boolean),
        Record.Field("i", None, Primitive.Int),
        Record.Field("l", None, Primitive.Long),
        Record.Field("f", None, Primitive.Float),
        Record.Field("d", None, Primitive.Double),
        Record.Field("ba", None, Primitive.Bytes),
        Record.Field("s", None, Primitive.String),
        Record.Field("n", None, Primitive.Null)
      )
    )
  )

  test[Enums](
    Record(
      Some("Enums"),
      None,
      List(
        Record.Field("e", None, Primitive.Enum(Some("Color"), None, List("Red", "Green", "Blue")))
      )
    )
  )

  {
    implicit val afDecimal = AvroField.bigDecimal(19, 0)
    test[Logical](
      Record(
        Some("Logical"),
        None,
        List(
          Record.Field("bd", None, Primitive.BigDecimal),
          Record.Field("u", None, Primitive.UUID)
        )
      )
    )
  }

  test[Date](
    Record(
      Some("Date"),
      None,
      List(Record.Field("d", None, Primitive.LocalDate))
    )
  )

  private val dateTimeSchema = Record(
    Some("DateTime"),
    None,
    List(
      Record.Field("i", None, Primitive.Instant),
      Record.Field("dt", None, Primitive.LocalDateTime),
      Record.Field("t", None, Primitive.LocalTime)
    )
  )

  {
    import magnolify.avro.logical.millis._
    test[DateTime]("millis", dateTimeSchema)
  }

  {
    import magnolify.avro.logical.micros._
    test[DateTime]("micros", dateTimeSchema)
  }

  {
    import magnolify.avro.logical.bigquery._
    test[DateTime]("BigQuery", dateTimeSchema)
  }

  test[Composite](
    Record(
      Some("Composite"),
      None,
      List(
        Record.Field("o", None, Optional(Primitive.Int)),
        Record.Field("l", None, Repeated(Primitive.Int)),
        Record.Field("m", None, Mapped(Primitive.String, Primitive.Int))
      )
    )
  )

  test[NullableArray](
    Record(
      Some("NullableArray"),
      None,
      List(
        Record.Field("a", None, Optional(Repeated(Primitive.Int)))
      )
    )
  )

  private val innerSchema =
    Record(
      Some("Inner"),
      None,
      List(Record.Field("i", None, Primitive.Int))
    )

  test[Outer](
    Record(
      Some("Outer"),
      None,
      List(
        Record.Field("r", None, innerSchema),
        Record.Field("o", None, Optional(innerSchema)),
        Record.Field("l", None, Repeated(innerSchema)),
        Record.Field("m", None, Mapped(Primitive.String, innerSchema))
      )
    )
  )
}

object AvroParserSuite {
  object Color extends Enumeration {
    val Red, Green, Blue = Value
  }

  case class Primitives(
    b: Boolean,
    i: Int,
    l: Long,
    f: Float,
    d: Double,
    ba: Array[Byte],
    s: String,
    n: Null
  )

  case class Enums(e: Color.Value)

  case class Logical(bd: BigDecimal, u: UUID)
  case class Date(d: LocalDate)
  case class DateTime(i: Instant, dt: LocalDateTime, t: LocalTime)

  case class Composite(o: Option[Int], l: List[Int], m: Map[String, Int])
  case class NullableArray(a: Option[List[Int]])

  case class Inner(i: Int)
  case class Outer(r: Inner, o: Option[Inner], l: List[Inner], m: Map[String, Inner])
}
