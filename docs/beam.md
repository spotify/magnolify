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

For date-time, instants, and durations, choose a **grouping** — which decides how `Instant` is encoded — and a **precision**:

* `import magnolify.beam.logical.timestamp.millis.*` (or `.micros`, `.nanos`) maps `Instant` to Beam's portable `Timestamp` logical type. This is what IcebergIO requires as of Beam 2.76.0.
* `import magnolify.beam.logical.compat.millis.*` (or `.micros`, `.nanos`) keeps the encodings magnolify produced through 0.9.7: the joda-backed `DATETIME` primitive at `millis`, a raw `INT64` of microseconds at `micros`, and the SDK-local `NanosInstant` logical type at `nanos`.

The bare `magnolify.beam.logical.millis.*`/`.micros`/`.nanos` objects still exist and still produce exactly what they produced in 0.9.7. They are deprecated aliases for the matching `compat` object, so **upgrading to 0.9.8 changes no schema until you change an import** — you get a deprecation warning telling you to pick a grouping explicitly.

Note that joda types have only millisecond resolution, so excess precision will be discarded when used with `micros` or `nanos`.

Where possible, Beam logical types are used and joda types defer to the java.time implementations:

* Beam's portable `Timestamp` logical type is used for java and joda `Instant` and the joda `DateTime` under `timestamp.*`, at the precision of the object you import: `Timestamp.MILLIS`, `Timestamp.MICROS` or `Timestamp.NANOS`.
* The `DateTime` logical type is used for millisecond-precision java and joda `LocalDateTime`
* The `Time` logical type is used for nanosecond-precision java and joda `LocalTime`
* The `NanosDuration` logical type is used for java and joda `Duration`

`LocalTime`, `LocalDateTime` and `Duration` are encoded identically under `timestamp` and `compat` at a given precision — Beam offers no joda schema type for them, so there is nothing to differ about. Only `Instant`, joda `Instant` and joda `DateTime` differ between the two groupings.

`Timestamp` rejects instants carrying finer precision than it declares rather than rounding them, so `timestamp.millis` and `timestamp.micros` truncate on write. An `Instant` with nanosecond precision written via `timestamp.micros` reads back truncated to microseconds. Use `timestamp.nanos` to preserve it.

Beam's `MicrosInstant` should not be used as it throws exceptions when presented with greater-than-microsecond precision data. `Timestamp.MICROS` is the safe equivalent.

### Choosing an encoding: which Beam IOs accept which

**Scope:** this section is about **Beam's own IOs consuming a `PCollection<Row>`** — that is, where the output of `RowType[T]` ends up. It says nothing about the IOs of frameworks built on Beam, which may share a name but not a code path. In Scio, for instance, only `IcebergIO`/`ManagedIO` take Beam `Row`s; its `BigQueryIO` and `AvroIO` route through magnolify's `bigquery` and `avro` modules, which target `TableRow` and `GenericRecord` directly, never a Beam `Schema`, and are unaffected by anything below.

Beam's IOs do not agree on a `Timestamp` precision, and several do not handle the type at all, so the grouping and precision you import determine where you can write. Verified against Beam 2.76.0:

| | `IcebergIO` | `BigQueryIO` | Avro extension | managed JDBC | Kafka `JSON` | Kafka `AVRO` | Beam SQL |
|---|---|---|---|---|---|---|---|
| `timestamp.millis` (precision 3) | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ † |
| `timestamp.micros` (precision 6) | **✓** | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ † |
| `timestamp.nanos` (precision 9) | ✗ | **✓** | **✓** | ✗ | ✗ | **✓** | ✓ † |
| `compat.millis` (`DATETIME`) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

† accepted at any precision, but Beam SQL round-trips timestamps through milliseconds (`BeamCalcRel.java:463`), so anything finer is silently dropped.

Where the rejections come from:

* `IcebergIO` requires precision 6 and throws `UnsupportedOperationException` otherwise (`IcebergUtils.java:227-234`).
* `BigQueryIO` and the Avro extension require precision 9 (`BigQueryUtils.java:591-596`, `BeamRowToStorageApiProto.java:252-260`, `AvroUtils.java:1227-1232`). Kafka with `AVRO` format goes through the same Avro check (`KafkaWriteSchemaTransformProvider.java:201`).
* The managed JDBC sinks (`postgres`, `mysql`, `sqlserver`) have no `Timestamp` branch at all. Unknown logical types fall back to their base type (`JdbcUtil.java:336-338`), and `Timestamp`'s base type is a `ROW`, which is not a writable JDBC type — so it throws `RuntimeException("ROW in schema is not supported while writing")`.
* Kafka with `JSON` format fails *late*: `RowJson` recurses into the `ROW` base type, finds only `INT64`/`INT16` inside, and so passes schema validation — then throws `ClassCastException` per element when it tries to cast the `java.time.Instant` to a `Row` (`RowJson.java:176-180`, `:593`).

How you would hit the JDBC and Kafka rows, since they are less obvious than Iceberg: Beam's schema transforms require a schema-bearing `PCollection<Row>`, and `RowCoder` is a `SchemaCoder`, so `pcoll.setCoder(RowCoder.of(rowType.schema))` is enough to make one. Any managed sink then accepts it.

Two consequences worth stating plainly:

**No `timestamp.*` precision reaches every sink, and some sinks accept none of them.** `compat.millis` is the only `Instant` encoding every schema-aware Beam IO accepts, because `DATETIME` is a primitive they all understand. If one record type needs multiple destinations, use `compat.millis` or define per-destination `RowType`s.

**`compat.micros` and `compat.nanos` are not general escape hatches.** `compat.micros` is a plain `INT64`, so it is structurally accepted nearly everywhere but lands as an integer column rather than a timestamp. `compat.nanos` is `NanosInstant`, whose base type is also a `ROW`, so it hits the same walls as `timestamp.*` — it is rejected by Iceberg (`IcebergUtils.java:236`) and was never writable there.

The Avro column is about `beam-sdks-java-extensions-avro` converting a Beam `Schema` to an Avro `Schema`. If you want Avro output from a case class, use magnolify's `avro` module instead — it has no Beam dependency and none of these constraints.

### Reading

Reads are more forgiving than writes, in a way worth knowing about. A `Timestamp` field surfaces as a `java.time.Instant` at any precision, so a reader whose declared precision is *finer or coarser* than the data still succeeds — it simply returns whatever precision the writer stored. Reading an Iceberg `timestamptz` (micros) with `timestamp.millis` yields a full microsecond `Instant`, with no truncation and no error; truncation applies on write only.

The one combination that fails loudly is a `compat.*` reader against `Timestamp`-encoded data, which throws `ClassCastException: java.time.Instant cannot be cast to org.joda.time.Instant`. That is the pairing contract below.

Reading is also where the groupings are least interchangeable, because most IOs still *produce* `DATETIME`. Beam's `BigQueryIO` is the clearest case: it demands precision 9 on write, but on read it returns `DATETIME` for an ordinary `TIMESTAMP` column and only yields a `Timestamp` logical type for a `TIMESTAMP(12)` column, under `--picosecondTimestampMapping` (`BigQueryUtils.java:474-493`). So no single import round-trips it — write with `timestamp.nanos`, read with `compat.millis`.

### The `compat` encodings

`compat.millis`, `compat.micros` and `compat.nanos` hold the `Instant` encodings magnolify produced through 0.9.7 — the joda-backed `DATETIME` primitive, a raw `INT64` of microseconds, and the `NanosInstant` logical type respectively. Non-instant mappings are identical to `timestamp.*`.

The name is deliberately about compatibility rather than representation: those three share no encoding, only the fact that this is what 0.9.7 emitted. `compat` is also not a deprecated holding pen — `compat.millis` is the correct and often the *only* choice for the destinations listed above, and stays correct as long as those IOs emit and accept `DATETIME`.

Use `compat.millis` when reading Rows that are still `DATETIME`-encoded — either because the connector hardcodes it (as of Beam 2.76.0 that includes jdbc, google-cloud-platform, clickhouse, delta, hcatalog, iceberg, singlestore and amazon-web-services2, plus core and the arrow, avro, sql and sql-datacatalog extensions), or because the pipeline pins Beam's `--updateCompatibilityVersion` below 2.76.0. `compat.millis` is the counterpart to that flag: pair them, or omit both. Setting the flag while using `timestamp.*` is the one combination that will not work.

Beyond the connectors that name `DATETIME` explicitly, Beam's schema inference maps any joda `Instant` field to `DATETIME`, so a connector whose element type has joda fields produces it too — KafkaIO's `KafkaSourceDescriptor` is one.

### Iceberg

Beam 2.76.0 changed IcebergIO's `timestamptz` mapping from `DATETIME` to `Timestamp.MICROS` in order to stop truncating microseconds. Use `timestamp.micros` — it is the only `Timestamp` precision IcebergIO accepts on write, and what it produces on read. `timestamp.millis` and `timestamp.nanos` will fail schema conversion with `UnsupportedOperationException`. If the pipeline pins `--updateCompatibilityVersion` below 2.76.0, use `compat.millis` instead.

Note the change was to the **read** path only; IcebergIO still accepts `DATETIME` on write (`IcebergUtils.java:77`), so `compat.millis` remains a valid way to write Iceberg — it just truncates to milliseconds, which is what prompted the Beam change in the first place.

## SQL types

**Deprecated since 0.9.8.** `magnolify.beam.logical.sql`'s `DATE`, `TIME` and `DATETIME` members duplicate those in `logical.date` and the precision objects, and its `TIMESTAMP` member is Beam's `MicrosInstant`, which throws on sub-microsecond instants. Use `logical.date` plus one of `timestamp.{millis,micros,nanos}` or `compat.{millis,micros,nanos}` instead.

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