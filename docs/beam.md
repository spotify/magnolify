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

## Enums
Enum-like types map to the Beam logical [Enum type]((https://beam.apache.org/documentation/programming-guide/#enumerationtype)). See @ref:[EnumType](enums.md) for more details. `UnsafeEnum[T]` instances are available from `import magnolify.beam.unsafe.*`.

## Time and dates

Java and joda `LocalDate` types are available via `import magnolify.beam.logical.date.*`

For date-time, instants, and durations, use `import magnolify.beam.logical.millis.*`, `import magnolify.beam.logical.micros.*` or `import magnolify.beam.logical.nanos.*` as appropriate for your use-case.
Note that joda types have only millisecond resolution, so excess precision will be discarded when used with `micros` or `nanos`.

Where possible, Beam logical types are used and joda types defer to these implementations:

* Beam's `DATETIME` primitive type maps to the millisecond-precision java and joda `Instant`s and the joda `DateTime`.
* The `DateTime` logical type is used for millisecond-precision java and joda `LocalDateTime`
* The `NanosInstant` logical type is used for nanosecond-precision java and joda `Instant`
* The `Time` logical type is used for nanosecond-precision java and joda `LocalTime`
* The `NanosDuration` logical type is used for java and joda `Duration`

Beam's `MicrosInstant` should not be used as it throws exceptions when presented with greater-than-microsecond precision data.

## SQL types

SQL-compatible logical types are supported via `import magnolify.beam.logical.sql.*`

## Case mapping

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