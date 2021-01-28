AvroType
========

`AvroType[T]` provides conversion between Scala type `T` and Avro `GenericRecord`. Custom support for type `T` can be added with an implicit instance of `AvroField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.avro._
import org.apache.avro.generic.GenericRecord

// Encode custom type URI as String
implicit val uriField = AvroField.from[String](URI.create)(_.toString)

val avroType = AvroType[Outer]
val genericRecord: GenericRecord = avroType.to(record)
val copy: Outer = avroType.from(genericRecord)

// Avro Schema
avroType.schema
```

Enum-like types map to Avro enums. See [enums.md](https://github.com/spotify/magnolify/tree/master/docs/enums.md) for more details. Additional `AvroField[T]` instances for `Byte`, `Char`, and `Short` are available from `import magnolify.avro.unsafe._`. These conversions are unsafe due to potential overflow.

To populate Avro type and field `doc`s, annotate the case class and its fields with the `@doc` annotation.

```scala
@doc("My record")
case class Record(@doc("int field") i: Int, @doc("string field") s: String)

@doc("My enum")
object Color extends Enumeration {
  type Type = Value
  value Red, Gree, Blue = Value
}
```

The `@doc` annotation can also be extended to support custom format.

```scala
class myDoc(doc: String, version: Int) extends doc(s"doc: $doc, version: $version")

@myDoc("My record", 2)
case class Record(@myDoc("int field", 1) i: Int, @myDoc("string field", 2) s: String)
```

To use a different field case format in target records, add an optional `CaseMapper` argument to `AvroType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val avroType = AvroType[LowerCamel](CaseMapper(toSnakeCase))
avroType.to(LowerCamel("John", "Doe"))
```

Avro `decimal` and `uuid` logical types map to `BigDecimal` and `java.util.UUID`. Additionally `decimal` requires `precision` and optional `scale` parameter.

```scala
implicit val afBigDecimal = AvroField.bigDecimal(20, 4)
```

Among the date/time types, `date` maps to `java.time.LocalDate`. The other types, `timestamp`, `time` and `local-timestamp`, map to `Instant`, `LocalTime` and `LocalDateTime` in either micro or milliseconds precision with `import magnolify.avro.logical.micros._` or `import magnolify.avro.logical.millis._`.

Map logical types to BigQuery compatible Avro with `import magnolify.avro.logical.bigquery._`.
