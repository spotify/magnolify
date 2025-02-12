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

## Case Mapping

To use a different field case format in target records, add an optional `CaseMapper` argument to `ParquetType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala mdoc:compile-only
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat
import magnolify.parquet._

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val parquetType = ParquetType[LowerCamel](CaseMapper(toSnakeCase))
```

## Enums

Enum-like types map to strings. See @ref:[EnumType](enums.md) for more details. Additional `ParquetField[T]` instances for `Char` and `UnsafeEnum[T]` are available from `import magnolify.parquet.unsafe._`. This conversions is unsafe due to potential overflow.

## Logical Types

Parquet `decimal` logical type maps to `BigDecimal` and supports the following encodings:

```scala mdoc:compile-only
import magnolify.parquet._

val pfDecimal32 = ParquetField.decimal32(9, 0)
val pfDecimal64 = ParquetField.decimal64(18, 0)
val pfDecimalFixed = ParquetField.decimalFixed(8, 18, 0)
val pfDecimalBinary = ParquetField.decimalBinary(20, 0)
```

For a full specification of Date/Time mappings in Parquet, see @ref:[Type Mappings](mapping.md).

## Parquet-Avro Compatibility

The official Parquet format specification supports [multiple valid schema representations of LIST types](https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists). Historically, magnolify-parquet has supported the simplest representation: simply marking the list element field as `REPEATED`, which per the spec defaults to a _required list field with required elements_. For example:

```scala mdoc
import magnolify.parquet._

case class RecordWithList(listField: List[Int])
ParquetType[RecordWithList].schema
```

Unfortunately, this schema isn't interoperable out-of-the-box with Parquet files produced by [parquet-avro](https://github.com/apache/parquet-java/tree/master/parquet-avro), which defaults to Parquet's 2-level list encoding (with a configurable option to use 3-level list encoding).

```scala mdoc
import org.apache.avro.Schema

// Avro schema matches `RecordWithList`
val avroSchema = new Schema.Parser().parse(s"""{
  "type": "record",
  "name": "RecordWithList",
  "fields": [
    {"name": "listField", "type": {"type": "array", "items": "int"}}
  ]
}""")

// Used by parquet-avro to convert Avro to Parquet schemas
import org.apache.parquet.avro.AvroSchemaConverter

// 2-level list encoding -- compare to schema generated for `RecordWithList` above
val convertedParquetSchema = new AvroSchemaConverter().convert(avroSchema)
```

Parquet-avro doesn't fully support the spec magnolify-parquet adheres to and can't interpret the Magnolify list schema. As a result, by default, if your schema contains a repeated type, **records produced by parquet-avro can't be consumed by magnolify-parquet, and vice versa**, unless you're using **Parquet-Avro Compatibility Mode**.

### Parquet-Avro Compatibility Mode

When Parquet-Avro Compatibility Mode is enabled, magnolify-parquet will interpret repeated fields using the same 2-level list structure that parquet-avro uses.
In addition, Parquet file writes will include an extra metadata key, `parquet.avro.schema`, to the file footer, containing the converted, String-serialized Avro schema.

#### Enabling Compatibility Mode on Magnolify < 0.8

You can enable this mode by importing `magnolify.parquet.ParquetArray.AvroCompat._` at the site where your `ParquetType[T]` is derived.
Note that you'll need to add this import for both writes (to produce 2-level encoded lists) _and_ reads (to consume 2-level encoded lists).

```scala mdoc:fail
import magnolify.parquet.ParquetArray.AvroCompat._

case class RecordWithList(listField: List[String])

val pt = ParquetType[RecordWithList]
```

#### Enabling Compatibility Mode on Magnolify >= 0.8

The `magnolify.parquet.ParquetArray.AvroCompat._` import is **deprecated** in Magnolify 0.8 and is expected to be removed in future versions.

Instead, in Magnolify 0.8 and above, this mode should be enabled on the _writer_ by setting a Hadoop `Configuration` option, `magnolify.parquet.write-grouped-arrays`.

```scala mdoc:reset
import org.apache.hadoop.conf.Configuration
import magnolify.parquet._

case class RecordWithList(listField: List[String])

val conf = new Configuration()
conf.setBoolean(MagnolifyParquetProperties.WriteAvroCompatibleArrays, true) // sets `magnolify.parquet.write-grouped-arrays`

// Instantiate ParquetType with configuration
val pt = ParquetType[RecordWithList](conf)

// Check that the converted Avro schema uses 2-level encoding
pt.schema
```

If you're a Scio user with `com.spotify:scio-parquet` on your classpath, you can instantiate a Configured `ParqueType` as a one-liner:

```scala mdoc:fail
import com.spotify.scio.parquet._
import magnolify.parquet._

case class RecordWithList(listField: List[String])

val pt = ParquetType[RecordWithList](
  ParquetConfiguration.of(MagnolifyParquetProperties.WriteAvroCompatibleArrays -> true)
)
```

You can combine a Configuration with a CaseMapper:

```scala mdcoc:compile-only
import magnolify.shared._

// Can be combined with a CaseMapper
val cm: CaseMapper = ???
ParquetType[RecordWithList](cm, conf)
```

If you don't have Hadoop on your classpath, you can instantiate a `MagnolifyParquetProperties` instance directly:

```scala mdoc:compile-only
import magnolify.parquet._

ParquetType[RecordWithList](new MagnolifyParquetProperties {
    override def WriteAvroCompatibleArrays: Boolean = true
  }
)
```

On the _reader_ side, 2-level arrays will be detected automatically based on the input file schema, so **no imports or extra Configurations are needed**.

## Field Descriptions

The top level class and all fields (including nested class fields) can be annotated with `@doc` annotation. Note that nested classes annotations are ignored.

```scala mdoc:compile-only
import magnolify.shared._

@doc("This is ignored")
case class NestedClass(@doc("nested field annotation") i: Int)

@doc("Top level annotation")
case class TopLevelType(@doc("field annotation") pd: NestedClass)
```

Note that field descriptions are *not* natively supported by the Parquet format. Instead, the `@doc` annotation ensures that the generated Avro schema written to the metadata key `parquet.avro.schema` will contain the specified field description:

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
import magnolify.shared._

@doc("Top level annotation")
case class MyRecord(@doc("field annotation") listField: List[Int])

// Note: If using Magnolify < 0.8, import magnolify.parquet.ParquetArray.AvroCompat._
// to ensure `parquet.avro.schema` metadata is written to file footer
val writer = ParquetType[MyRecord]
  .writeBuilder(HadoopOutputFile.fromPath(path, new Configuration()))
  .build()
writer.write(MyRecord(List(1,2,3)))
writer.close()

// Note that Parquet MessageType schema doesn't contain descriptor, but serialized Avro schema does
ParquetFileReader.open(HadoopInputFile.fromPath(path, new Configuration())).getFileMetaData
```

Note: On Magnolify < 0.8, you must enable [Avro compatibility mode](#parquet-avro-compatibility-mode) via the `AvroCompat` import if you're using the `@doc` annotation with ParquetType,
which triggers magnolify-parquet to write a translated Avro schema to the file footer metadata key `parquet.avro.schema`. Otherwise, your annotations will be essentially thrown out.
On Magnolify >= 0.8, this key is written by default.
