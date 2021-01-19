ExampleType
===========

`ExampleType[T]` provides conversion between Scala type `T` and TensorFlow `Example`. Custom support for type `T` can be added with an implicit instance of `ExampleField[T]`.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.tensorflow._
import com.google.protobuf.ByteString
import org.tensorflow.proto.example.{Example, Example.Builder}

// Encode custom type String/URI as ByteString
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
implicit val uriField = ExampleField
  .from[ByteString](b => URI.create(b.toStringUtf8))(u => ByteString.copyFromUtf8(u.toString))

val exampleType = ExampleType[Outer]
val exampleBuilder: Example.Builder = exampleType.to(record)
val copy = exampleType.from(exampleBuilder.build)
```

`ExampleType` encodes each field in a `Feature` of the same name. It encodes nested fields by joining field names as `field_a.field_b.field_c`. Optional and repeated types are not supported in a nested field.

Additional `ExampleField[T]` instances for `Byte`, `Char`, `Short`, `Int`, `Double`, `Boolean`, `String`, and enum-like types are available from `import magnolify.tensorflow.unsafe._`. These conversions are unsafe due to potential overflow and encoding errors.  Enum-like types map to strings. See [enums.md](https://github.com/spotify/magnolify/tree/master/docs/enums.md) for more details.

To use a different field case format in target records, add an optional `CaseMapper` argument to `ExampleType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val exampleType = ExampleType[LowerCamel](CaseMapper(toSnakeCase))
exampleType.to(LowerCamel("John", "Doe"))
```
