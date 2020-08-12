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
import org.tensorflow.example.{Example, Example.Builder}

// Encode custom type String/URI as ByteString
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
implicit val uriField = ExampleField
  .from[ByteString](b => URI.create(b.toStringUtf8))(u => ByteString.copyFromUtf8(u.toString))

val exampleType = ExampleType[Outer]
val exampleBuilder: Example.Builder = exampleType.to(record)
val copy = exampleType.from(exampleBuilder.build)
```

`ExampleType` encodes each field in a `Feature` of the same name. It encodes nested fields by joining field names as `field_a.field_b.field_c`. Optional and repeated types are not supported in a nested field.

Additional `ExampleField[T]` instances for `Byte`, `Char`, `Short`, `Int`, `Double`, `Boolean`, `String`, Java `Enum` and Scala `Enumeration` are available from `import magnolify.tensorflow.unsafe._`. These conversions are unsafe due to potential overflow and encoding errors.

To use a different field case format in target records, add an optional `CaseMapper` argument to `ExampleType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert _
val exampleType = ExampleType[LowerCamel](CaseMapper(toSnakeCase))
exampleType.to(LowerCamel("John", "Doe"))
```

`CaseMapper` supports enums too.

```scala
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

import magnolify.shared._
// Encode as ["red", "green", "blue"]
implicit val enumType = EnumType[Color.Type].map(CaseMapper(_.toLowerCase))
```
