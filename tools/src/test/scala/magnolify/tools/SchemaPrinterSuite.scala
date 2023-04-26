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

import magnolify.test._

class SchemaPrinterSuite extends MagnolifySuite {
  private def test(schema: Record, code: String): Unit =
    assertEquals(SchemaPrinter.print(schema).trim, code.trim)

  test("Primitive") {
    List(
      Primitive.Null,
      Primitive.Boolean,
      Primitive.Char,
      Primitive.Byte,
      Primitive.Short,
      Primitive.Int,
      Primitive.Long,
      Primitive.Float,
      Primitive.Double,
      Primitive.String,
      Primitive.Bytes,
      Primitive.BigInt,
      Primitive.BigDecimal,
      Primitive.Instant,
      Primitive.LocalDateTime,
      Primitive.OffsetTime,
      Primitive.LocalTime,
      Primitive.LocalDate,
      Primitive.UUID
    ).foreach { p =>
      test(
        Record(Some("Primitive"), None, None, List(Field("f", None, p, Required))),
        s"case class Primitive(f: $p)"
      )
    }
  }

  test("Repetition") {
    List(
      Required -> "%s",
      Optional -> "Option[%s]",
      Repeated -> "List[%s]"
    ).foreach { case (r, f) =>
      test(
        Record(Some("Repetition"), None, None, List(Field("f", None, Primitive.Int, r))),
        s"case class Repetition(f: ${f.format("Int")})"
      )
    }
  }

  test("Enum") {
    val anonymous = Enum(None, None, None, List("Red", "Green", "Blue"))
    test(
      Record(Some("Enum"), None, None, List(Field("color_enum", None, anonymous, Required))),
      """case class Enum(color_enum: Enum.ColorEnum)
        |
        |object Enum {
        |  object ColorEnum extends Enumeration {
        |    type Type = Value
        |    val Red, Green, Blue = Value
        |  }
        |}
        |""".stripMargin
    )

    val named = anonymous.copy(name = Some("Color"))
    test(
      Record(Some("Enum"), None, None, List(Field("color_enum", None, named, Required))),
      """case class Enum(color_enum: Enum.Color)
        |
        |object Enum {
        |  object Color extends Enumeration {
        |    type Type = Value
        |    val Red, Green, Blue = Value
        |  }
        |}
        |""".stripMargin
    )
  }

  test("Record") {
    val anonymous = Record(None, None, None, List(Field("f", None, Primitive.Int, Required)))
    test(
      Record(Some("Record"), None, None, List(Field("inner_record", None, anonymous, Required))),
      """case class Record(inner_record: Record.InnerRecord)
        |
        |object Record {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    val named = anonymous.copy(name = Some("Inner"))
    test(
      Record(Some("Record"), None, None, List(Field("inner_record", None, named, Required))),
      """case class Record(inner_record: Record.Inner)
        |
        |object Record {
        |  case class Inner(f: Int)
        |}
        |""".stripMargin
    )
  }

  test("UpperCamel") {
    val inner = Record(None, None, None, List(Field("f", None, Primitive.Int, Required)))
    val field = Field(null, None, inner, Required)

    // lower_underscore
    test(
      Record(Some("Outer"), None, None, List(field.copy(name = "inner_record"))),
      """case class Outer(inner_record: Outer.InnerRecord)
        |
        |object Outer {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    // UPPER_UNDERSCORE
    test(
      Record(Some("Outer"), None, None, List(field.copy(name = "INNER_RECORD"))),
      """case class Outer(INNER_RECORD: Outer.InnerRecord)
        |
        |object Outer {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    // lowerCamel
    test(
      Record(Some("Outer"), None, None, List(field.copy(name = "innerRecord"))),
      """case class Outer(innerRecord: Outer.InnerRecord)
        |
        |object Outer {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    // UpperCamel
    test(
      Record(Some("Outer"), None, None, List(field.copy(name = "InnerRecord"))),
      """case class Outer(InnerRecord: Outer.InnerRecord)
        |
        |object Outer {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    // lower-hyphen
    test(
      Record(Some("Outer"), None, None, List(field.copy(name = "inner-record"))),
      """case class Outer(`inner-record`: Outer.InnerRecord)
        |
        |object Outer {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )
  }

  test("Deduplication") {
    val level3 =
      Record(Some("Level3"), None, None, List(Field("i3", None, Primitive.Int, Required)))
    val level2 = Record(
      Some("Level2"),
      None,
      None,
      List(
        Field("r2", None, level3, Required),
        Field("o2", None, level3, Optional),
        Field("l2", None, level3, Repeated)
      )
    )
    val level1 = Record(
      Some("Level1"),
      None,
      None,
      List(
        Field("r1", None, level2, Required),
        Field("o1", None, level2, Optional),
        Field("l1", None, level2, Repeated)
      )
    )

    test(
      level1,
      """case class Level1(r1: Level1.Level2, o1: Option[Level1.Level2], l1: List[Level1.Level2])
        |
        |object Level1 {
        |  case class Level2(r2: Level2.Level3, o2: Option[Level2.Level3], l2: List[Level2.Level3])
        |
        |  object Level2 {
        |    case class Level3(i3: Int)
        |  }
        |}
        |""".stripMargin
    )
  }
}
