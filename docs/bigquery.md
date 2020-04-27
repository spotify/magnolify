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

Additional `TableRowField[T]` instances for `Byte`, `Char`, `Short`, `Int`, and `Float` are available from `import magnolify.bigquery.unsafe._`. These conversions are unsafe due to potential overflow.
