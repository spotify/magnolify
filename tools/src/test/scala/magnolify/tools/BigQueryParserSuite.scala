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
      List(
        Record.Field("b", None, Primitive.Boolean),
        Record.Field("l", None, Primitive.Long),
        Record.Field("d", None, Primitive.Double),
        Record.Field("ba", None, Primitive.Bytes),
        Record.Field("s", None, Primitive.String),
        Record.Field("bd", None, Primitive.BigDecimal)
      )
    )
  )

  test[DateTime](
    Record(
      None,
      None,
      List(
        Record.Field("i", None, Primitive.Instant),
        Record.Field("dt", None, Primitive.LocalDateTime),
        Record.Field("d", None, Primitive.LocalDate),
        Record.Field("t", None, Primitive.LocalTime)
      )
    )
  )

  test[Repetitions](
    Record(
      None,
      None,
      List(
        Record.Field("o", None, Optional(Primitive.Long)),
        Record.Field("l", None, Repeated(Primitive.Long))
      )
    )
  )

  private val innerSchema =
    Record(
      None,
      None,
      List(Record.Field("l", None, Primitive.Long))
    )

  test[Outer](
    Record(
      None,
      None,
      List(
        Record.Field("r", None, innerSchema),
        Record.Field("o", None, Optional(innerSchema)),
        Record.Field("l", None, Repeated(innerSchema))
      )
    )
  )
}

object BigQueryParserSuite {
  case class Primitives(b: Boolean, l: Long, d: Double, ba: Array[Byte], s: String, bd: BigDecimal)

  case class DateTime(i: Instant, dt: LocalDateTime, d: LocalDate, t: LocalTime)

  case class Repetitions(o: Option[Long], l: List[Long])

  case class Inner(l: Long)
  case class Outer(r: Inner, o: Option[Inner], l: List[Inner])
}
