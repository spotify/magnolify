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

## List Encodings

The official Parquet format supports [multiple valid list encodings](https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists).

However, not all format implementations are guaranteed to support all valid list encodings interchangeably. Therefore, Magnolify supports multiple list encoding options.

Consider a list of required strings:

```scala mdoc:reset
case class RecordWithList(listField: List[String])
```

When converting to Parquet, the most common legacy encoding is a repeated group named `array` containing a single element field:

```
message RecordWithList {
    required group listField (LIST) {
      repeated group array {
        required binary s (STRING);
      };
    }
}
```

However, the recommended format is a three-level list structure encoding a repeated group `list` and a required or optional `element`:

```
message RecordWithList {
    required group listField (LIST) {
      repeated group list {
        required binary element (STRING);
      }
    }
}
```

Finally, an ungrouped `repeated` field will be interpeted as a required list with non-null elements:

```
message RecordWithList {
    repeated binary listField (STRING);
}
```

Magnolify-parquet offers support for all three of these encodings:

|                 | Ungrouped   | Old List Format | New List Format | Configuration                                                            |
|-----------------|-------------|-----------------|-----------------|--------------------------------------------------------------------------|
| Magnolify < 0.8 | x (Default) | x               |                 | AvroCompat import                                                        |
| Magnolify 0.8   | x (Default) | x               |                 | AvroCompat import (Deprecated); `magnolify.parquet.write-grouped-arrays` |
| Magnolify > 0.8 | x (Default) | x               | x               | AvroCompat import (Deprecated); `magnolify.parquet.write-array-encoding` |

If left unspecified, magnolify-parquet will use ungrouped list encoding:

```scala mdoc:reset
import magnolify.parquet._

case class RecordWithList(listField: List[String])

ParquetType[RecordWithList].schema
```

### AvroCompat import

AvroCompat is a now-deprecated import used to enable the old list format:

```scala mdoc:reset
import magnolify.parquet._
import magnolify.parquet.ParquetArray.AvroCompat._

case class RecordWithList(listField: List[String])

@scala.annotation.nowarn("cat=deprecation")
val pt = ParquetType[RecordWithList]

// Schema uses old list encoding
pt.schema
```

### Property-based configuration

As of Magnolify 0.8, the `AvroCompat` import is deprecated; it will default to old list format.

In Magnolify 0.8, list encoding should be configured via a `MagnolifyParquetProperties` instance, specifically:

```scala
ParquetType[RecordWithList](new MagnolifyParquetProperties {
  // Overriding writeAvroCompatibleArrays to true enables old list format
  override def writeAvroCompatibleArrays: ArrayEncoding = true
})
```

In Magnolify > 0.8, list encoding should be configured via a `MagnolifyParquetProperties` instance, specifically:

```scala mdoc:reset
import magnolify.parquet._

case class RecordWithList(listField: List[String])

implicit val pt = ParquetType[RecordWithList](new MagnolifyParquetProperties {
  override def writeArrayEncoding: ArrayEncoding = ArrayEncoding.NewListEncoding
})

// Check that the converted Avro schema uses modern 3-level encoding
pt.schema
```

If your project includes a custom `core-site.xml` its in resources directory, you can also set Parquet list encoding through a Hadoop configuration property:

```xml
<configuration>
  <property>
    <name>magnolify.parquet.write-array-encoding</name>
    <value>new-list-encoding</value> # Also supported: `ungrouped`, `old-list-encoding`
  </property>
</configuration>
```

```scala mdoc:silent
import org.apache.hadoop.conf.Configuration
import magnolify.parquet._

// Default Configuration constructor picks up core-site value
ParquetType[RecordWithList](new Configuration())
```

Or, if you're using scio-parquet, you can use Scio's `ParquetConfiguration` API:

```scala mdoc:fail:silent
import com.spotify.scio.parquet._
import magnolify.parquet._

ParquetType[RecordWithList](
  ParquetConfiguration.of(MagnolifyParquetProperties.WriteArrayFormat -> MagnolifyParquetProperties.NewListEncoding)
)
```

Ungrouped is the default array encoding; any other encoding option must be configured on both the reader and writer typeclasses.

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
