ParquetType
===========

`ParquetType[T]` provides read and write support between Scala type `T` and the Parquet columnar storage format. Custom support for type `T` can be added with an implicit instance of `ParquetField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.parquet._

// Encode custom type URI as String
implicit val uriField = ParquetField.from[String](b => URI.create(b))(_.toString)

val parquetType = ParquetType[Outer]

// Parquet schema
parquetType.schema
```

Use `ParquetType#readBuilder` and `ParquetType#writeBuilder` to create new file reader and writer instances. See [HadoopSuite.scala](https://github.com/spotify/magnolify/tree/master/parquet/src/test/scala/magnolify/parquet/test/HadoopSuite.scala) for examples with Hadoop IO.

Enum-like types map to strings. See [enums.md](https://github.com/spotify/magnolify/tree/master/docs/enums.md) for more details. Additional `ParquetField[T]` instance for `Char` is available from `import magnolify.parquet.unsafe._`. This conversions is unsafe due to potential overflow.

To use a different field case format in target records, add an optional `CaseMapper` argument to `ParquetType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val parquetType = ParquetType[LowerCamel](CaseMapper(toSnakeCase))
```

Parquet `decimal` logical type maps to `BigDecimal` and supports the following encodings:

```scala
implicit val pfDecimal32 = ParquetField.decimal32(9, 0)
implicit val pfDecimal64 = ParquetField.decimal64(18, 0)
implicit val pfDecimalFixed = ParquetField.decimalFixed(8, 18, 0)
implicit val pfDecimalBinary = ParquetField.decimalBinary(20, 0)
```

Among the date/time types, `DATE` maps to `java.time.LocalDate`. The other types, `TIME` and `TIMESTAMP`, map to `OffsetTime`/`LocalTime` and `Instant`/`LocalDateTime` with `isAdjustedToUTC` set to `true`/`false`. They can be in nano, micro, or milliseconds precision with `import magnolify.parquet.logical.{nanos,micros,millis}._`.

Note that Parquet's official Avro support maps `REPEATED` fields to an `array` field inside a nested group. Use `import magnolify.parquet.ParquetArray.AvroCompat._` to ensure compatibility with Avro.