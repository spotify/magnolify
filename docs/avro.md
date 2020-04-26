AvroType
========

`AvroType[T]` provides convertion between Scala type `T` and Avro `GenericRecord`. Custom support for type `T` can be added with an implicit instance of `AvroField[T]`.

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

Additional `AvroField[T]` instances for `Byte`, `Char`, and `Short` are available from `import magnolify.avro.unsafe._`. These conversions are unsafe due to potential overflow.
