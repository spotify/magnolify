# Beam

`BeamSchemaType[T]` provides conversion between Scala type `T` and a [Beam Schema](https://beam.apache.org/documentation/programming-guide/#schema-definition). Custom support for type `T` can be added with an implicit intsance of `BeamSchemaField[T]`.

```scala mdoc:compile-only
import java.net.URI

case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.beam.*
// Encode custom type URI as String
implicit val uriField: BeamSchemaField[URI] = BeamSchemaField.from[String](URI.create)(_.toString)

val beamSchemaType = BeamSchemaType[Outer]
val row = beamSchemaType.to(record)
val copy: Outer = beamSchemaType.from(row)

// Beam Schema
val schema = beamSchemaType.schema
```

Enum-like types map to the Beam logical [Enum type]((https://beam.apache.org/documentation/programming-guide/#enumerationtype)). See @ref:[EnumType](enums.md) for more details. `UnsafeEnum[T]` instances are available from `import magnolify.beam.unsafe._`.

To use a different field case format in target records, add an optional `CaseMapper` argument to `BeamSchemaType`:

```scala mdoc:compile-only
import magnolify.beam.*
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val beamSchemaType = BeamSchemaType[LowerCamel](CaseMapper(toSnakeCase))
beamSchemaType.to(LowerCamel("John", "Doe")) // Row(first_name: John, last_name: Doe)
```

Use `import magnolify.beam.logical.millis._`, `import magnolify.beam.logical.micros._` or `import magnolify.beam.logical.nanos._` as appropriate for your use-case.
Beam's `DATETIME` type maps to the millisecond-precision `java.time.Instant`.
Beam's `DateTime` logical type is used for millisecond-precision `java.time.LocalDateTime`, the `NanosInstant` logical type for nanosecond-precision `java.time.Instant`, the `Time` logical type for nanosecond-precision `java.time.LocalTime`, and the `NanosDuration` logical type for `java.time.Duration`.