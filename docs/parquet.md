# Parquet

`ParquetType[T]` provides read and write support between Scala type `T` and the Parquet columnar storage format. Custom support for type `T` can be added with an implicit instance of `ParquetField[T]`.

```scala mdoc:compile-only
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.parquet._

// Encode custom type URI as String
implicit val uriField: ParquetField[URI] = ParquetField.from[String](b => URI.create(b))(_.toString)

val parquetType = ParquetType[Outer]

// Parquet schema
val schema = parquetType.schema
```

Use `ParquetType#readBuilder` and `ParquetType#writeBuilder` to create new file reader and writer instances. See [HadoopSuite.scala](https://github.com/spotify/magnolify/tree/master/parquet/src/test/scala/magnolify/parquet/test/HadoopSuite.scala) for examples with Hadoop IO.

Enum-like types map to strings. See @ref:[EnumType](enums.md) for more details. Additional `ParquetField[T]` instances for `Char` and `UnsafeEnum[T]` are available from `import magnolify.parquet.unsafe._`. This conversions is unsafe due to potential overflow.

To use a different field case format in target records, add an optional `CaseMapper` argument to `ParquetType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala mdoc:compile-only
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat
import magnolify.parquet._

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val parquetType = ParquetType[LowerCamel](CaseMapper(toSnakeCase))
```

Parquet `decimal` logical type maps to `BigDecimal` and supports the following encodings:

```scala mdoc:compile-only
import magnolify.parquet._

val pfDecimal32 = ParquetField.decimal32(9, 0)
val pfDecimal64 = ParquetField.decimal64(18, 0)
val pfDecimalFixed = ParquetField.decimalFixed(8, 18, 0)
val pfDecimalBinary = ParquetField.decimalBinary(20, 0)
```

Among the date/time types, `DATE` maps to `java.time.LocalDate`. The other types, `TIME` and `TIMESTAMP`, map to `OffsetTime`/`LocalTime` and `Instant`/`LocalDateTime` with `isAdjustedToUTC` set to `true`/`false`. They can be in nano, micro, or milliseconds precision with `import magnolify.parquet.logical.{nanos,micros,millis}._`.

## Avro compatibility

The official Parquet format specification supports the `REPEATED` modifier to denote array types. By default, magnolify-parquet conforms to this specification:

```scala mdoc
import magnolify.parquet._

case class MyRecord(listField: List[Int])
ParquetType[MyRecord].schema
```

However, the parquet-avro API encodes array types differently: as a nested array inside a required group.

```scala mdoc
import org.apache.avro.Schema
val avroSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"MyRecord\",\"fields\":[{\"name\": \"listField\", \"type\": {\"type\": \"array\", \"items\": \"string\"}}]}")

import org.apache.parquet.avro.AvroSchemaConverter
new AvroSchemaConverter().convert(avroSchema)
```

Due to this discrepancy, **by default, a Repeated type (i.e. a `List` or `Seq`) written by parquet-avro isn't readable by magnolify-parquet, and vice versa**.

To address this, magnolify-parquet supports an "Avro compatibility mode" that, when enabled, will:

- Use the same Repeated schema format as parquet-avro
- Write an additional metadata key, `parquet.avro.schema`, to the Parquet file footer, containing the equivalent Avro schema.

You can enable this mode by importing `magnolify.parquet.ParquetArray.AvroCompat._`:

```scala mdoc:reset
import magnolify.parquet._
import magnolify.parquet.ParquetArray.AvroCompat._

case class MyRecord(listField: List[Int])
// List schema matches parquet-avro spec
ParquetType[MyRecord].schema

// This String value of this schema will be written to the Parquet metadata key `parquet.avro.schema`
ParquetType[MyRecord].avroSchema
```

## Field descriptions

The top level class and all fields (including nested class fields) can be annotated with `@doc` annotation. Note that nested classes annotations are ignored.

```scala mdoc:compile-only
import magnolify.shared._

@doc("This is ignored")
case class NestedClass(@doc("nested field annotation") i: Int)

@doc("Top level annotation")
case class TopLevelType(@doc("field annotation") pd: NestedClass)
```

Note that field descriptions are *not* natively supported by the Parquet format. Instead, the `@doc` annotation ensures
that in Avro compat mode, the generated Avro schema written to the metadata key `parquet.avro.schema` will contain the specified field description:

```scala mdoc:reset:invisible
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.hadoop.util._
import org.apache.parquet.hadoop.ParquetFileReader
import java.nio.file.Files

val path = new Path(Files.createTempDirectory("parquet-tmp").toFile.getAbsolutePath, "tmp.parquet")
```

```scala mdoc
import magnolify.parquet._
// AvroCompat is required to write `parquet.avro.schema` key to file metadata
import magnolify.parquet.ParquetArray.AvroCompat._
import magnolify.shared._

@doc("Top level annotation")
case class MyRecord(@doc("field annotation") listField: List[Int])

val writer = ParquetType[MyRecord]
  .writeBuilder(HadoopOutputFile.fromPath(path, new Configuration()))
  .build()
writer.write(MyRecord(List(1,2,3)))
writer.close()

ParquetFileReader.open(HadoopInputFile.fromPath(path, new Configuration())).getFileMetaData
```

**Therefore, enabling [Avro compatibility mode](#avro-compatibility) via the `AvroCompat` import is required to use the `@doc` annotation with ParquetType.**
