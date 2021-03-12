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

import magnolify.bigquery._
import magnolify.test._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.reflect.ClassTag

class BigQueryParserSuite extends MagnolifySuite {
  import BigQueryParserSuite._

  private def test[T: ClassTag](schema: Record)(implicit t: TableRowType[T]): Unit =
    test[T](null, schema)

  private def test[T: ClassTag](suffix: String, schema: Record)(implicit
    t: TableRowType[T]
  ): Unit = {
    val name = className[T] + (if (suffix == null) "" else "." + suffix)
    test(name) {
      assertEquals(BigQueryParser.parse(t.schema), schema)
    }
  }

  test[Primitives](
    Record(
      None,
      None,
      None,
      List(
        "b" -> Primitive.Boolean,
        "l" -> Primitive.Long,
        "d" -> Primitive.Double,
        "ba" -> Primitive.Bytes,
        "s" -> Primitive.String,
        "bd" -> Primitive.BigDecimal
      ).map(kv => Field(kv._1, None, kv._2, Required))
    )
  )

  test[DateTime](
    Record(
      None,
      None,
      None,
      List(
        "i" -> Primitive.Instant,
        "dt" -> Primitive.LocalDateTime,
        "d" -> Primitive.LocalDate,
        "t" -> Primitive.LocalTime
      ).map(kv => Field(kv._1, None, kv._2, Required))
    )
  )

  test[Repetitions](
    Record(
      None,
      None,
      None,
      List(
        "r" -> Required,
        "o" -> Optional,
        "l" -> Repeated
      ).map(kv => Field(kv._1, None, Primitive.Long, kv._2))
    )
  )

  private val innerSchema =
    Record(
      None,
      None,
      None,
      List(Field("l", None, Primitive.Long, Required))
    )

  test[Outer](
    Record(
      None,
      None,
      None,
      List(
        "r" -> Required,
        "o" -> Optional,
        "l" -> Repeated
      ).map(kv => Field(kv._1, None, innerSchema, kv._2))
    )
  )
}

object BigQueryParserSuite {
  case class Primitives(b: Boolean, l: Long, d: Double, ba: Array[Byte], s: String, bd: BigDecimal)

  case class DateTime(i: Instant, dt: LocalDateTime, d: LocalDate, t: LocalTime)

  case class Repetitions(r: Long, o: Option[Long], l: List[Long])

  case class Inner(l: Long)
  case class Outer(r: Inner, o: Option[Inner], l: List[Inner])
}
