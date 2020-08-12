BigtableType
============

`BigtableType[T]` provides conversion between Scala type `T` and Bigtable `Row`/`Seq[Mutation]` for read/write. Custom support for type `T` can be added with an implicit instance of `BigtableField[T]`.

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
val copy: Outer = bigtableType(row, "ColumnFamily")
```

`BigtableType` encodes each field in a separate column qualifier of the same name. It encodes nested fields by joining field names as `field_a.field_b.field_c`. Repeated types are not supported.

To use a different field case format in target records, add an optional `CaseMapper` argument to `BigtableType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert _
val bigtableType = BigtableType[LowerCamel](CaseMapper(toSnakeCase))
bigtableType(LowerCamel("John", "Doe"), "cf")
```

Java `enum` and Scala `Enumeration` types are encoded as strings and support `CaseMapper` too.

```scala
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

import magnolify.shared._
// Encode as ["red", "green", "blue"]
implicit val enumType = EnumType[Color.Type].map(CaseMapper(_.toLowerCase))
```
