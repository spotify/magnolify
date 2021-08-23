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

import com.google.common.base.CaseFormat
import org.typelevel.paiges._

object SchemaPrinter {
  def print(schema: Record, width: Int = 100): String = renderRecord(schema).renderTrim(width)

  private def renderRecord(schema: Record): Doc = {
    val name = schema.name.get
    val header = Doc.text("case class") + Doc.space + Doc.text(name) + Doc.char('(')
    val footer = Doc.char(')')
    val body =
      Doc.intercalate(
        Doc.char(',') + Doc.lineOrSpace,
        schema.fields.map(renderField(name, _))
      )
    val caseClass = body.tightBracketBy(header + Doc.lineOrEmpty, Doc.lineOrEmpty + footer)
    val companion = renderCompanion(name, schema.fields)
    caseClass + companion
  }

  private def renderCompanion(name: String, fields: List[Field]): Doc = {
    val header = Doc.text("object") + Doc.space + Doc.text(name) + Doc.space + Doc.char('{')
    val footer = Doc.char('}')
    val nestedFields = fields
      .flatMap { f =>
        f.schema match {
          case record: Record =>
            val n = record.name.orElse(Some(toUpperCamel(f.name)))
            Some(n.get -> renderRecord(record.copy(name = n)))
          case enum: Enum =>
            val n = enum.name.orElse(Some(toUpperCamel(f.name)))
            Some(n.get -> renderEnum(enum.copy(name = n)))
          case _ =>
            None
        }
      }
      .groupBy(_._1)
      .map { case (k, vs) =>
        val docs = vs.map(_._2).toSet
        require(
          docs.size == 1,
          s"Conflicting nested type $k:\n${docs.map(_.renderTrim(80)).mkString("\n")}"
        )
        docs.head
      }
      .toList

    if (nestedFields.isEmpty) {
      Doc.empty
    } else {
      val body = Doc.intercalate(Doc.hardLine * 2, nestedFields)
      Doc.hardLine * 2 + nested(header, body, footer)
    }
  }

  private def renderEnum(schema: Enum): Doc = {
    val header = Doc.text("object") + Doc.space + Doc.text(schema.name.get) + Doc.space +
      Doc.text("extends") + Doc.space + Doc.text("Enumeration") + Doc.space +
      Doc.char('{')
    val footer = Doc.char('}')
    val eq = Doc.space + Doc.char('=') + Doc.space
    val values = Doc.intercalate(Doc.char(',') + Doc.lineOrSpace, schema.values.map(Doc.text))
    val bodyLines = List(
      Doc.text("type") + Doc.space + Doc.text("Type") + eq + Doc.text("Value"),
      Doc.text("val") + Doc.space + values + eq + Doc.text("Value")
    )
    val body = Doc.intercalate(Doc.hardLine, bodyLines)

    nested(header, body, footer)
  }

  private def renderField(name: String, field: Field): Doc = {
    val rawType = field.schema match {
      case p: Primitive => Doc.text(p.toString)
      case r: Record    => Doc.text(name + "." + r.name.getOrElse(toUpperCamel(field.name)))
      case e: Enum      => Doc.text(name + "." + e.name.getOrElse(toUpperCamel(field.name)))
    }
    val tpe = field.repetition match {
      case Required => rawType
      case Optional => Doc.text("Option") + Doc.char('[') + rawType + Doc.char(']')
      case Repeated => Doc.text("List") + Doc.char('[') + rawType + Doc.char(']')
    }
    Doc.text(quoteIdentifier(field.name)) + Doc.char(':') + Doc.space + tpe
  }

  private def nested(header: Doc, body: Doc, footer: Doc): Doc =
    header + (Doc.hardLine + body).nested(2) + Doc.hardLine + footer

  private def quoteIdentifier(id: String): String = {
    import scala.reflect.internal.Chars
    if (id.headOption.exists(Chars.isIdentifierStart) && id.tail.forall(Chars.isIdentifierPart)) {
      id
    } else {
      s"`$id`"
    }
  }

  private def toUpperCamel(name: String): String = {
    var allLower = true
    var allUpper = true
    var hasHyphen = false
    var hasUnderscore = false
    name.foreach { c =>
      if (c.isLower) allUpper = false
      if (c.isUpper) allLower = false
      if (c == '-') hasHyphen = true
      if (c == '_') hasUnderscore = true
    }
    val format = (hasHyphen, hasUnderscore) match {
      case (false, false) =>
        if (name.head.isUpper) CaseFormat.UPPER_CAMEL else CaseFormat.LOWER_CAMEL
      case (true, false) if allLower => CaseFormat.LOWER_HYPHEN
      case (false, true) if allLower => CaseFormat.LOWER_UNDERSCORE
      case (false, true) if allUpper => CaseFormat.UPPER_UNDERSCORE
      case _ => throw new IllegalArgumentException(s"Unsupported case format: $name")
    }

    format.to(CaseFormat.UPPER_CAMEL, name)
  }
}
