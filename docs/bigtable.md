BigtableType
============

`BigtableType[T]` provides convertion between Scala type `T` and Bigtable `Row`/`Seq[Mutation]` for read/write. Custom support for type `T` can be added with an implicit instance of `BigtableField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.bigtable._
import com.google.bigtable.v2.{Mutation, Row}

// Encode custom type URI as String
implicit val uriField = BigtableField.from[String](URI.create)(_.toString)

val bigtableType = BigtableType[Outer]
val mutations: Seq[Mutation] = bigtableType(record, "ColumnFamily")
val row: Row = BigtableType.mutationsToRow(ByteString.copyFromUtf8("RowKey"), mutations)
val copy: Outer = bigtableType.from(row, "ColumnFamily")
```

`BigtableType` encodes each field in a separate column qualifier of the same name. It encodes nested fields by joining field names as `field_a.field_b.field_c`. Repeated types are not supported.
