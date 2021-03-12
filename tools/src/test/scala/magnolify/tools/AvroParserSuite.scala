/*
 * Copyright 2021 Spotify AB.
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

  private val namespace = Some(this.getClass.getCanonicalName)

  test[Primitives](
    Record(
      Some("Primitives"),
      namespace,
      None,
      List(
        "b" -> Primitive.Boolean,
        "i" -> Primitive.Int,
        "l" -> Primitive.Long,
        "f" -> Primitive.Float,
        "d" -> Primitive.Double,
        "ba" -> Primitive.Bytes,
        "s" -> Primitive.String,
        "u" -> Primitive.Unit
      ).map(kv => Field(kv._1, None, kv._2, Required))
    )
  )

  test[Enums](
    Record(
      Some("Enums"),
      namespace,
      None,
      List(
        Field(
          "e",
          None,
          Enum(
            Some("Color"),
            namespace,
            None,
            List("Red", "Green", "Blue")
          ),
          Required
        )
      )
    )
  )

  {
    implicit val afDecimal = AvroField.bigDecimal(19, 0)
    test[Logical](
      Record(
        Some("Logical"),
        namespace,
        None,
        List(
          Field("bd", None, Primitive.BigDecimal, Required),
          Field("u", None, Primitive.UUID, Required)
        )
      )
    )
  }

  test[Date](
    Record(
      Some("Date"),
      namespace,
      None,
      List(Field("d", None, Primitive.LocalDate, Required))
    )
  )

  private val dateTimeSchema = Record(
    Some("DateTime"),
    namespace,
    None,
    List(
      "i" -> Primitive.Instant,
      "dt" -> Primitive.LocalDateTime,
      "t" -> Primitive.LocalTime
    ).map(kv => Field(kv._1, None, kv._2, Required))
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

  test[Repetitions](
    Record(
      Some("Repetitions"),
      namespace,
      None,
      List(
        "r" -> Required,
        "o" -> Optional,
        "l" -> Repeated
      ).map(kv => Field(kv._1, None, Primitive.Int, kv._2))
    )
  )

  private val innerSchema =
    Record(
      Some("Inner"),
      namespace,
      None,
      List(Field("i", None, Primitive.Int, Required))
    )

  test[Outer](
    Record(
      Some("Outer"),
      namespace,
      None,
      List(
        "r" -> Required,
        "o" -> Optional,
        "l" -> Repeated
      ).map(kv => Field(kv._1, None, innerSchema, kv._2))
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
    u: Unit
  )

  case class Enums(e: Color.Value)

  case class Logical(bd: BigDecimal, u: UUID)
  case class Date(d: LocalDate)
  case class DateTime(i: Instant, dt: LocalDateTime, t: LocalTime)

  case class Repetitions(r: Int, o: Option[Int], l: List[Int])

  case class Inner(i: Int)
  case class Outer(r: Inner, o: Option[Inner], l: List[Inner])
}
