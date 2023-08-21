EnumType
========

`EnumType[T]` provides conversion between enum-like types and their string names. Supported enum-like types are Java `enum`, Scala `Enumeration`, and `sealed trait` with `case object`s. `AvroType[T]` and `ProtobufType[T]` use it to map to their native enum types, i.e. `EnumSymbol` and `ProtocolMessageEnum`, while other converters map to strings.

`CaseMapper` supports enums too.

```scala mdoc
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

import magnolify.shared._
// Encode as ["red", "green", "blue"]
implicit val enumType: EnumType[Color.Type] = EnumType.scalaEnumType[Color.Type].map(CaseMapper(_.toLowerCase))
```

An enum-like type can be wrapped inside a `magnolify.shared.UnsafeEnum[T]` to handle unknown cases. This could be useful for scenarios like schema evolution, or working with bad data.

```scala mdoc
UnsafeEnum(Color.Red)
UnsafeEnum.from[Color.Type]("Red")
UnsafeEnum.from[Color.Type]("Purple")
```
