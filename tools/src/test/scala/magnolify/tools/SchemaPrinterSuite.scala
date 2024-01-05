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

class SchemaPrinterSuite extends munit.ScalaCheckSuite {
  private def test(schema: Record, code: String): Unit =
    assertEquals(SchemaPrinter.print(schema).trim, code.trim)

  test("Root") {
    test(
      Record(None, None, List(Record.Field("f", None, Primitive.Int))),
      "case class Root(f: Int)"
    )
  }

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
        Record(Some("Primitive"), None, List(Record.Field("f", None, p))),
        s"case class Primitive(f: $p)"
      )
    }
  }

  test("Composite") {
    List(
      Optional(Primitive.Int) -> "Option[Int]",
      Repeated(Primitive.Int) -> "List[Int]",
      Mapped(Primitive.String, Primitive.Int) -> "Map[String, Int]"
    ).foreach { case (schema, expected) =>
      test(
        Record(Some("Composite"), None, List(Record.Field("f", None, schema))),
        s"case class Composite(f: $expected)"
      )
    }
  }

  test("Enum") {
    val anonymous = Primitive.Enum(None, None, List("Red", "Green", "Blue"))
    test(
      Record(Some("Enum"), None, List(Record.Field("color_enum", None, anonymous))),
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
      Record(Some("Enum"), None, List(Record.Field("color_enum", None, named))),
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
    val anonymous = Record(None, None, List(Record.Field("f", None, Primitive.Int)))
    test(
      Record(Some("Record"), None, List(Record.Field("inner_record", None, anonymous))),
      """case class Record(inner_record: Record.InnerRecord)
        |
        |object Record {
        |  case class InnerRecord(f: Int)
        |}
        |""".stripMargin
    )

    val named = anonymous.copy(name = Some("Inner"))
    test(
      Record(Some("Record"), None, List(Record.Field("inner_record", None, named))),
      """case class Record(inner_record: Record.Inner)
        |
        |object Record {
        |  case class Inner(f: Int)
        |}
        |""".stripMargin
    )
  }

  test("Deduplication") {
    val level3 = Record(Some("Level3"), None, List(Record.Field("i3", None, Primitive.Int)))
    val level2 = Record(
      Some("Level2"),
      None,
      List(
        Record.Field("r2", None, level3),
        Record.Field("o2", None, Optional(level3)),
        Record.Field("l2", None, Repeated(level3))
      )
    )
    val level1 = Record(
      Some("Level1"),
      None,
      List(
        Record.Field("r1", None, level2),
        Record.Field("o1", None, Optional(level2)),
        Record.Field("l1", None, Repeated(level2))
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

  test("UpperCamel") {
    // lower_underscore
    assertEquals(SchemaPrinter.toUpperCamel("inner_record"), "InnerRecord")

    // UPPER_UNDERSCORE
    assertEquals(SchemaPrinter.toUpperCamel("INNER_RECORD"), "InnerRecord")

    // lowerCamel
    assertEquals(SchemaPrinter.toUpperCamel("innerRecord"), "InnerRecord")

    // UpperCamel
    assertEquals(SchemaPrinter.toUpperCamel("InnerRecord"), "InnerRecord")

    // lower-hyphen
    assertEquals(SchemaPrinter.toUpperCamel("inner-record"), "InnerRecord")
  }
}
