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

import com.google.common.base.CaseFormat
import org.typelevel.paiges._

object SchemaPrinter {
  private case class RenderContext(field: String, owner: Option[String])

  private val RootContext = RenderContext("root", None)

  def print(schema: Record, width: Int = 100): String =
    renderRecord(RootContext)(schema).renderTrim(width)

  private def renderRecord(ctx: RenderContext)(schema: Record): Doc = {
    val name = schema.name.getOrElse(toUpperCamel(ctx.field))

    val header = Doc.text("case class") + Doc.space + Doc.text(name) + Doc.char('(')
    val body = Doc.intercalate(
      Doc.char(',') + Doc.lineOrSpace,
      schema.fields.map { f =>
        val fieldCtx = RenderContext(f.name, Some(name))
        val param = quoteIdentifier(f.name)
        val tpe = renderType(fieldCtx)(f.schema)
        Doc.text(param) + Doc.char(':') + Doc.space + tpe
      }
    )
    val footer = Doc.char(')')
    val caseClass = body.tightBracketBy(header + Doc.lineOrEmpty, Doc.lineOrEmpty + footer)

    val companion = renderCompanion(name, schema.fields)
    caseClass + companion
  }

  private def renderCompanion(name: String, fields: List[Record.Field]): Doc = {
    val header = Doc.text("object") + Doc.space + Doc.text(name) + Doc.space + Doc.char('{')
    val footer = Doc.char('}')
    val nestedFields = fields
      .flatMap { f =>
        f.schema match {
          case record: Record =>
            Some(record.name -> renderRecord(RenderContext(f.name, Some(name)))(record))
          case enum: Primitive.Enum =>
            Some(enum.name -> renderEnum(RenderContext(f.name, Some(name)))(enum))
          case _ => None
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

  private def renderEnum(ctx: RenderContext)(schema: Primitive.Enum): Doc = {
    val name = schema.name.getOrElse(toUpperCamel(ctx.field))
    val header = Doc.text("object") + Doc.space + Doc.text(name) + Doc.space +
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
  private def renderOwnerPrefix(ctx: RenderContext): Doc =
    ctx.owner.fold(Doc.empty)(o => Doc.text(o) + Doc.char('.'))

  private def renderType(ctx: RenderContext)(s: Schema): Doc = s match {
    case Optional(s) =>
      Doc.text("Option") + Doc.char('[') + renderType(ctx)(s) + Doc.char(']')
    case Repeated(s) =>
      Doc.text("List") + Doc.char('[') + renderType(ctx)(s) + Doc.char(']')
    case Mapped(k, v) =>
      val keyType = renderType(ctx)(k)
      val valueType = renderType(ctx)(v)
      Doc.text("Map") + Doc.char('[') + keyType + Doc.char(',') + Doc.space + valueType + Doc.char(
        ']'
      )
    case Record(name, _, _) =>
      renderOwnerPrefix(ctx) + Doc.text(name.getOrElse(toUpperCamel(ctx.field)))
    case Primitive.Enum(name, _, _) =>
      renderOwnerPrefix(ctx) + Doc.text(name.getOrElse(toUpperCamel(ctx.field)))
    case p: Primitive =>
      Doc.text(p.toString)
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

  private[tools] def toUpperCamel(name: String): String = {
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
