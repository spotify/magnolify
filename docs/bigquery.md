TableRowType
============

`TableRowType[T]` provides conversion between Scala type `T` and BigQuery `TableRow`. Custom support for type `T` can be added with an implicit instance of `TableRowField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.bigquery._
import com.google.api.services.bigquery.model.TableRow

// Encode custom type URI as String
implicit val uriField = TableRowField.from[String](URI.create)(_.toString)

val tableRowType = TableRowType[Outer]
val tableRow: TableRow = tableRowType.to(record)
val copy: Outer = tableRowType.from(tableRow)

// BigQuery TableSchema
tableRowType.schema
```

Additional `TableRowField[T]` instances for `Byte`, `Char`, `Short`, `Int`, `Float`, Java `Enum` and Scala `Enumeration` are available from `import magnolify.bigquery.unsafe._`. These conversions are unsafe due to potential overflow or encoding errors.

To populate BigQuery table and field `description`s, annotate the case class and its fields with the `@description` annotation.

```scala
@description("My record")
case class Record(@description("int field") i: Int, @description("string field") s: String)
```

The `@description` annotation can also be extended to support custom format.

```scala
class myDesc(description: String, version: Int)
  extends description(s"description: description, version: $version")

@myDesc("My record", 2)
case class Record(@myDesc("int field", 1) i: Int, @myDesc("string field", 2) s: String)
```

To use a different field case format in target records, add an optional `CaseMapper` argument to `TableRowType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert _
val tableRowType = TableRowType[LowerCamel](CaseMapper(toSnakeCase))
tableRowType.to(LowerCamel("John", "Doe"))
```

`CaseMapper` supports enums too.

```scala
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

import magnolify.shared._
// Encode as ["red", "green", "blue"]
implicit val enumType = EnumType[Color.Type](CaseMapper(_.toLowerCase))
```
