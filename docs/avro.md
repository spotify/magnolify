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

Additional `AvroField[T]` instances for `Byte`, `Char`, and `Short` are available from `import magnolify.avro.unsafe._`. These conversions are unsafe due to potential overflow.

To populate Avro type and field `doc`s, annotate the case class and its fields with the `@doc` annotation.

```scala
@doc("My record")
case class Record(@doc("int field") i: Int, @doc("string field") s: String)
```

The `@doc` annotation can also be extended to support custom format.

```scala
class myDoc(doc: String, version: Int) extends doc(s"doc: $doc, version: $version")

@myDoc("My record", 2)
case class Record(@myDoc("int field", 1) i: Int, @myDoc("string field", 2) s: String)
```
