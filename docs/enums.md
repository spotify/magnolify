EnumType
========

`EnumType[T]` provides conversion between enum-like types and their string names. Supported enum-like types are Java `enum`, Scala `Enumeration`, and `sealed trait` with `case object`s. `AvroType[T]` and `ProtobufType[T]` use it to map to their native enum types, i.e. `EnumSymbol` and `ProtocolMessageEnum`, while other converters map to strings.

`CaseMapper` supports enums too.

```scala
object Color extends Enumeration {
  type Type = Value
  val Red, Green, Blue = Value
}

import magnolify.shared._
// Encode as ["red", "green", "blue"]
implicit val enumType = EnumType[Color.Type](CaseMapper(_.toLowerCase))
```
