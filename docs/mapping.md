# Type Mapping

| Scala                             | Avro                         | BigQuery               | Bigtable<sup>7</sup>            | Datastore             | Parquet                           | Protobuf                | TensorFlow          |
|-----------------------------------|------------------------------|------------------------|---------------------------------|-----------------------|-----------------------------------|-------------------------|---------------------|
| `Unit`                            | `NULL`                       | x                      | x                               | `Null`                | x                                 | x                       | x                   |
| `Boolean`                         | `BOOLEAN`                    | `BOOL`                 | `Byte`                          | `Boolean`             | `BOOLEAN`                         | `Boolean`               | `INT64`<sup>3</sup> |
| `Char`                            | `INT`<sup>3</sup>            | `INT64`<sup>3</sup2>   | `Char`                          | `Integer`<sup>3</sup> | `INT32`<sup>3</sup>               | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Byte`                            | `INT`<sup>3</sup>            | `INT64`<sup>3</sup2>   | `Byte`                          | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>               | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Short`                           | `INT`<sup>3</sup>            | `INT64`<sup>3</sup2>   | `Short`                         | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>               | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Int`                             | `INT`                        | `INT64`<sup>3</sup2>   | `Int`                           | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>               | `Int`                   | `INT64`<sup>3</sup> |
| `Long`                            | `LONG`                       | `INT64`                | `Long`                          | `Integer`             | `INT64`<sup>9</sup>               | `Long`                  | `INT64`             |
| `Float`                           | `FLOAT`                      | `FLOAT64`<sup>3</sup2> | `Float`                         | `Double`<sup>3</sup>  | `FLOAT`                           | `Float`                 | `FLOAT`             |
| `Double`                          | `DOUBLE`                     | `FLOAT64`              | `Double`                        | `Double`              | `DOUBLE`                          | `Double`                | `FLOAT`<sup>3</sup> |
| `CharSequence`                    | `STRING`                     | x                      | x                               | x                     | x                                 | x                       | x                   |
| `String`                          | `STRING`                     | `STRING`               | `String`                        | `String`              | `BINARY`                          | `String`                | `BYTES`<sup>3</sup> |
| `Array[Byte]`                     | `BYTES`                      | `BYTES`                | `ByteString`                    | `Blob`                | `BINARY`                          | `ByteString`            | `BYTES`             |
| `ByteString`                      | x                            | x                      | `ByteString`                    | `Blob`                | x                                 | `ByteString`            | `BYTES`             |
| `ByteBuffer`                      | `BYTES`                      | x                      | x                               |                       | x                                 | x                       | x                   |
| Enum<sup>1</sup>                  | `ENUM`                       | `STRING`<sup>3</sup2>  | `String`                        | `String`<sup>3</sup>  | `BINARY`/`ENUM`<sup>9</sup>       | Enum                    | `BYTES`<sup>3</sup> |
| `BigInt`                          | x                            | x                      | `BigInt`                        | x                     | x                                 | x                       | x                   |
| `BigDecimal`                      | `BYTES`<sup>4</sup>          | `NUMERIC`<sup>6</sup2> | `Int` scale + unscaled `BigInt` | x                     | `LOGICAL[DECIMAL]`<sup>9,14</sup> | x                       | x                   |
| `Option[T]`                       | `UNION[NULL, T]`<sup>5</sup> | `NULLABLE`             | Empty as `None`                 | Absent as `None`      | `OPTIONAL`                        | `optional`<sup>10</sup> | Size <= 1           |
| `Iterable[T]`<sup>2</sup>         | `ARRAY`                      | `REPEATED`             | x                               | `Array`               | `REPEATED`<sup>13</sup>           | `repeated`              | Size >= 0           |
| Nested                            | `RECORD`                     | `STRUCT`               | Flat<sup>8</sup>                | `Entity`              | Group                             | `Message`               | Flat<sup>8</sup>    |
| `Map[CharSequence, T]`            | `MAP[STRING, T]`             | x                      | x                               | x                     | x                                 | x                       |                     |
| `Map[String, T]`                  | `MAP[STRING, T]`             | x                      | x                               | x                     | x                                 | x                       | x                   |
| `java.time.Instant`               | `LONG`<sup>11</sup>          | `TIMESTAMP`            | x                               | `Timestamp`           | `LOGICAL[TIMESTAMP]`<sup>9</sup>  | x                       | x                   |
| `java.time.LocalDateTime`         | `LONG`<sup>11</sup>          | `DATETIME`             | x                               | x                     | `LOGICAL[TIMESTAMP]`<sup>9</sup>  | x                       | x                   |
| `java.time.OffsetTime`            | x                            | x                      | x                               | x                     | `LOGICAL[TIME]`<sup>9</sup>       | x                       | x                   |
| `java.time.LocalTime`             | `LONG`<sup>11</sup>          | `TIME`                 | x                               | x                     | `LOGICAL[TIME]`<sup>9</sup>       | x                       | x                   |
| `java.time.LocalDate`             | `INT`<sup>11</sup>           | `DATE`                 | x                               | x                     | `LOGICAL[DATE]`<sup>9</sup>       | x                       | x                   |
| `org.joda.time.LocalDate`         | `INT`<sup>11</sup>           | x                      | x                               | x                     | x                                 | x                       | x                   |
| `org.joda.time.DateTime`          | `INT`<sup>11</sup>           | x                      | x                               | x                     | x                                 | x                       | x                   |
| `org.joda.time.LocalTime`         | `INT`<sup>11</sup>           | x                      | x                               | x                     | x                                 | x                       | x                   |
| `java.util.UUID`                  | `STRING`<sup>4</sup>         | x                      | ByteString (16 bytes)           | x                     | `FIXED[16]`                       | x                       | x                   |
| `(Long, Long, Long)`<sup>12</sup> | `FIXED[12]`                  | x                      | x                               | x                     | x                                 | x                       | x                   |

1. Those wrapped in`UnsafeEnum` are encoded as strings,
   see [enums.md](https://github.com/spotify/magnolify/blob/master/docs/enums.md) for more
2. Any subtype of `Iterable[T]`
3. Unsafe conversions, `import magnolify.$MODULE.unsafe._`
4. Avro logical types ([doc](https://avro.apache.org/docs/current/spec.html#Logical+Types))
5. `UNION` of `[NULL, T]` and defaults to `NULL` ([doc](https://avro.apache.org/docs/current/spec.html#Unions))
6. Fixed precision of 38 and scale of
   9 ([doc](https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type))
7. All Scala types are encoded as big endian `ByteString` for Bigtable
8. Nested fields are encoded flat with field names joined with `.`, e.g. `level1.level2.level3`
9. More information on Parquet logical type schemas can be
   found [here](https://github.com/apache/parquet-format/blob/master/LogicalTypes.md). Time types are available at
   multiple precisions; import `magnolify.parquet.logical.micros._`, `magnolify.avro.logical.millis._`,
   or `magnolify.avro.logical.nanos._` accordingly.
10. See [protobuf.md](https://github.com/spotify/magnolify/blob/master/docs/protobuf.md) for more
11. Logical types available at micro- or milli-second precision; import `magnolify.avro.logical.micros._`
    or `magnolify.avro.logical.millis._` accordingly. BigQuery-compatible conversions are available
    in `magnolify.avro.logical.bigquery._`.
12. Special tuple used to represent Duration in the [Avro spec](https://avro.apache.org/docs/1.11.0/spec.html#Duration).
    This has not been made implicit in Magnolify; import `AvroType.afDuration` implicitly to enable
13. If `magnolify.parquet.ParquetArray.AvroCompat._` is imported, array fields use the nested, Avro-compatible schema
    format: `required group $FIELDNAME (LIST) { repeated $FIELDTYPE array ($FIELDSCHEMA); }`.
14. Parquet's Decimal logical format supports multiple representations, and are not implicitly scoped by default. Import
    one of: `magnolify.parquet.ParquetField.{decimal32, decimal64, decimalFixed, decimalBinary}`.
