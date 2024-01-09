# Protobuf

`ProtobufType[T, MsgT]` provides conversion between Scala type `T` and Protobuf `MsgT <: Message`. Custom support for type `T` can be added with an implicit instance of `ProtobufField[T]`.

```scala mdoc:compile-only
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

// Protobuf record
abstract class MyProto extends com.google.protobuf.Message

import magnolify.protobuf._

// Encode custom type URI as String
implicit val uriField = ProtobufField.from[String](URI.create)(_.toString)
 
// MyProto is a compiled Protobuf Message
val protobufType = ProtobufType[Outer, MyProto]
val proto: MyProto = protobufType.to(record)
val copy: Outer = protobufType.from(proto)
```

Enum like types map to Protobuf enums. See @ref:[EnumType](enums.md) for more details. An implicit instance from Java or Scala type to Protobuf enum must be provided.

```scala mdoc:compile-only
// Scala enum
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

// Protobuf enum
// enum ColorProto {
//   RED = 0;
//   GREEN = 1;
//   BLUE = 2;
// }
abstract class ColorProto(name: String, ordinal: Int) extends Enum[ColorProto](name, ordinal) with com.google.protobuf.ProtocolMessageEnum

import magnolify.protobuf._
implicit val efEnum = ProtobufField.enum[Color.Type, ColorProto]
```

Additional `ProtobufField[T]` instances for `Byte`, `Char`, `Short`, and `UnsafeEnum[T]` are available from `import magnolify.protobuf.unsafe._`. These conversions are unsafe due to potential overflow.

To use a different field case format in target records, add an optional `CaseMapper` argument to `ProtobufType`. The following example maps `firstName` & `lastName` to `first_name` & `last_name`.

```scala mdoc:compile-only
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat
import magnolify.protobuf._

case class LowerCamel(firstName: String, lastName: String)

// Protobuf record
abstract class LowerHyphenProto extends com.google.protobuf.Message

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val protobufType = ProtobufType[LowerCamel, LowerHyphenProto](CaseMapper(toSnakeCase))
protobufType.to(LowerCamel("John", "Doe"))
```
