# Beam

`RowType[T]` provides conversion between Scala type `T` and a Beam Row, backed by a [Beam Schema](https://beam.apache.org/documentation/programming-guide/#schema-definition). Custom support for type `T` can be added with an implicit instance of `RowField[T]`.

```scala mdoc:compile-only
import java.net.URI

case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

import magnolify.beam.*
// Encode custom type URI as String
implicit val uriField: RowField[URI] = RowField.from[String](URI.create)(_.toString)

val rowType = RowType[Outer]
val row = rowType.to(record)
val copy: Outer = rowType.from(row)

// Beam Schema
val schema = rowType.schema
```

## Enums
Enum-like types map to the Beam logical [Enum type]((https://beam.apache.org/documentation/programming-guide/#enumerationtype)). See @ref:[EnumType](enums.md) for more details. `UnsafeEnum[T]` instances are available from `import magnolify.beam.unsafe.*`.

## Time and dates

Java and joda `LocalDate` types are available via `import magnolify.beam.logical.date.*`

For date-time, instants, and durations, use `import magnolify.beam.logical.millis.*`, `import magnolify.beam.logical.micros.*` or `import magnolify.beam.logical.nanos.*` as appropriate for your use-case.
Note that joda types have only millisecond resolution, so excess precision will be discarded when used with `micros` or `nanos`.

Where possible, Beam logical types are used and joda types defer to the java.time implementations:

* Beam's portable `Timestamp` logical type is used for java and joda `Instant` and the joda `DateTime`, at the precision of the object you import: `Timestamp.MILLIS`, `Timestamp.MICROS` or `Timestamp.NANOS`.
* The `DateTime` logical type is used for millisecond-precision java and joda `LocalDateTime`
* The `Time` logical type is used for nanosecond-precision java and joda `LocalTime`
* The `NanosDuration` logical type is used for java and joda `Duration`

`Timestamp` rejects instants carrying finer precision than it declares rather than rounding them, so `millis` and `micros` truncate on write. An `Instant` with nanosecond precision written via `micros` reads back truncated to microseconds. Use `nanos` to preserve it.

Beam's `MicrosInstant` should not be used as it throws exceptions when presented with greater-than-microsecond precision data. `Timestamp.MICROS` is the safe equivalent.

### Choosing a precision: which IOs accept which

Beam IOs that validate `Timestamp` precision do not agree on one, so the precision you import determines which IOs you can write to. As of Beam 2.76.0:

| | IcebergIO | BigQueryIO | Avro extension |
|---|---|---|---|
| `millis` (`Timestamp.MILLIS`, precision 3) | ✗ | ✗ | ✗ |
| `micros` (`Timestamp.MICROS`, precision 6) | **✓** | ✗ | ✗ |
| `nanos` (`Timestamp.NANOS`, precision 9) | ✗ | **✓** | **✓** |
| `legacy.*` (`DATETIME`) | ✓ | ✓ | ✓ |

IcebergIO requires precision 6 and throws `UnsupportedOperationException` otherwise; BigQueryIO and Beam's Avro extension require precision 9 and throw `IllegalArgumentException`/`RuntimeException` otherwise.

Note the consequence: **no single precision object is writable to both Iceberg and BigQuery.** Before 0.10, `millis` mapped `Instant` to `DATETIME`, which all of them accept, so one import served every destination. If you need one record type to reach both, either keep `legacy.*` or define per-destination `RowType`s.

### Reading

Reads are more forgiving than writes, in a way worth knowing about. A `Timestamp` field surfaces as a `java.time.Instant` at any precision, so a reader whose declared precision is *finer or coarser* than the data still succeeds — it simply returns whatever precision the writer stored. Reading an Iceberg `timestamptz` (micros) with `millis` yields a full microsecond `Instant`, with no truncation and no error; truncation applies on write only.

The one combination that fails loudly is a `legacy.*` reader against `Timestamp`-encoded data, which throws `ClassCastException: java.time.Instant cannot be cast to org.joda.time.Instant`. That is the pairing contract below.

### Pre-0.10 encodings

Before 0.10, `Instant` mapped to Beam's joda-backed `DATETIME` primitive under `millis`, a raw `INT64` of microseconds under `micros`, and the `NanosInstant` logical type under `nanos`. Those encodings are still available via `import magnolify.beam.logical.legacy.millis.*` (or `legacy.micros`, `legacy.nanos`). Non-instant mappings are identical to the defaults.

Use `legacy` when reading Rows that are still `DATETIME`-encoded — either because the connector hardcodes it (as of Beam 2.76.0 that includes jdbc, google-cloud-platform, clickhouse, delta, hcatalog, iceberg, singlestore and amazon-web-services2, plus core and the arrow, avro, sql and sql-datacatalog extensions), or because the pipeline pins Beam's `--updateCompatibilityVersion` below 2.76.0. `legacy` is the counterpart to that flag: pair them, or omit both. Setting the flag while using the default objects is the one combination that will not work.

Beyond the connectors that name `DATETIME` explicitly, Beam's schema inference maps any joda `Instant` field to `DATETIME`, so a connector whose element type has joda fields produces it too — KafkaIO's `KafkaSourceDescriptor` is one.

### Iceberg

Beam 2.76.0 changed IcebergIO's `timestamptz` mapping from `DATETIME` to `Timestamp.MICROS` in order to stop truncating microseconds. Use `micros` — it is the only precision IcebergIO accepts on write, and what it produces on read. `millis` and `nanos` are not Iceberg-writable and will fail schema conversion with `UnsupportedOperationException`. If the pipeline pins `--updateCompatibilityVersion` below 2.76.0, use `legacy.millis` instead.

## SQL types

**Deprecated since 0.10.** `magnolify.beam.logical.sql`'s `DATE`, `TIME` and `DATETIME` members duplicate those in `logical.date` and the precision objects, and its `TIMESTAMP` member is Beam's `MicrosInstant`, which throws on sub-microsecond instants. Use `logical.date` plus one of `millis`/`micros`/`nanos` instead.

## Case mapping

To use a different field case format in target records, add an optional `CaseMapper` argument to `RowType`:

```scala mdoc:compile-only
import magnolify.beam.*
import magnolify.shared.CaseMapper
import com.google.common.base.CaseFormat

case class LowerCamel(firstName: String, lastName: String)

val toSnakeCase = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert _
val rowType = RowType[LowerCamel](CaseMapper(toSnakeCase))
rowType.to(LowerCamel("John", "Doe")) // Row(first_name: John, last_name: Doe)
```