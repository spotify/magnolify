Type Mapping
============

| Scala                     | Avro                     | BigQuery               | Bigtable<sup>7</sup>            | Datastore             | Parquet                         | Protobuf                | TensorFlow          |
| ------------------------- | ------------------------ | ---------------------- | ------------------------------- | --------------------- | ------------------------------- | ----------------------- | ------------------- |
| `Unit`                    | `NULL`                   | -                      | -                               | `Null`                | -                               | -                       | -                   |
| `Boolean`                 | `BOOLEAN`                | `BOOL`                 | `Byte`                          | `Boolean`             | `BOOLEAN`                       | `Boolean`               | `INT64`<sup>3</sup> |
| `Char`                    | `INT`<sup>3</sup>        | `INT64`<sup>3</sup2>   | `Char`                          | `Integer`<sup>3</sup> | `INT32`<sup>3</sup>             | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Byte`                    | `INT`<sup>3</sup>        | `INT64`<sup>3</sup2>   | `Byte`                          | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>             | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Short`                   | `INT`<sup>3</sup>        | `INT64`<sup>3</sup2>   | `Short`                         | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>             | `Int`<sup>3</sup>       | `INT64`<sup>3</sup> |
| `Int`                     | `INT`                    | `INT64`<sup>3</sup2>   | `Int`                           | `Integer`<sup>3</sup> | `INT32`<sup>9</sup>             | `Int`                   | `INT64`<sup>3</sup> |
| `Long`                    | `LONG`                   | `INT64`                | `Long`                          | `Integer`             | `INT64`<sup>9</sup>             | `Long`                  | `INT64`             |
| `Float`                   | `FLOAT`                  | `FLOAT64`<sup>3</sup2> | `Float`                         | `Double`<sup>3</sup>  | `FLOAT`                         | `Float`                 | `FLOAT`             |
| `Double`                  | `DOUBLE`                 | `FLOAT64`              | `Double`                        | `Double`              | `DOUBLE`                        | `Double`                | `FLOAT`<sup>3</sup> |
| `String`                  | `STRING`                 | `STRING`               | `String`                        | `String`              | `BINARY` + `STRING`<sup>9</sup> | `String`                | `BYTES`<sup>3</sup> |
| `Array[Byte]`             | `BYTES`                  | `BYTES`                | `ByteString`                    | `Blob`                | `BINARY`                        | `ByteString`            | `BYTES`             |
| `ByteString`              | -                        | -                      | `ByteString`                    | `Blob`                | -                               | `ByteString`            | `BYTES`             |
| Enum<sup>1</sup>          | `ENUM`                   | `STRING`<sup>3</sup2>  | `String`                        | `String`<sup>3</sup>  | `BINARY` + `ENUM`<sup>9</sup>   | Enum                    | `BYTES`<sup>3</sup> |
| `BigInt`                  | -                        | -                      | `BigInt`                        | -                     | -                               | -                       | -                   |
| `BigDecimal`              | `BYTES`<sup>4</sup>      | `NUMERIC`<sup>6</sup2> | `Int` scale + unscaled `BigInt` | -                     | Int/Binary/Fixed<sup>9</sup>    | -                       | -                   |
| `Option[T]`               | `UNION`<sup>5</sup>      | `NULLABLE`             | Empty as `None`                 | Absent as `None`      | `OPTIONAL`                      | `optional`<sup>10</sup> | Size <= 1           |
| `Iterable[T]`<sup>2</sup> | `ARRAY`                  | `REPEATED`             | -                               | `Array`               | `REPEATED`                      | `repeated`              | Size >= 0           |
| Nested                    | `RECORD`                 | `STRUCT`               | Flat<sup>8</sup>                | `Entity`              | Group                           | `Message`               | Flat<sup>8</sup>    |
| `Map[String, T]`          | `MAP`                    | -                      | -                               | -                     | -                               | -                       | -                   |
| `Instant`                 | `INT`<sup>4</sup>        | `TIMESTAMP`            | -                               | `Integer`             | `INT64`<sup>9</sup>             | -                       | -                   |
| `LocalDateTime`           | `LONG`<sup>4</sup>       | `DATETIME`             | -                               | -                     | `INT64`<sup>9</sup>             | -                       | -                   |
| `OffsetTime`              | -                        | -                      | -                               | -                     | `INT32`/`INT64`<sup>9</sup>     | -                       | -                   |
| `LocalTime`               | `INT`/`LONG`<sup>4</sup> | `TIME`                 | -                               | -                     | `INT32`/`INT64`<sup>9</sup>     | -                       | -                   |
| `LocalDate`               | `INT`<sup>4</sup>        | `DATE`                 | -                               | -                     | `INT32`<sup>9</sup>             | -                       | -                   |
| `UUID`                    | `STRING`<sup>4</sup>     | -                      | 16 bytes                        | -                     | Fixed<sup>9</sup>               | -                       | -                   |

1. Those wrapped in`UnsafeEnum` are encoded as strings, see [enums.md](https://github.com/spotify/magnolify/blob/master/docs/enums.md) for more
2. Any subtype of `Iterable[T]`
3. Unsafe conversions, `import magnolify.$MODULE.unsafe._`
4. Avro logical types ([doc](https://avro.apache.org/docs/current/spec.html#Logical+Types))
5. `UNION` of `[NULL, T]` and defaults to `NULL` ([doc](https://avro.apache.org/docs/current/spec.html#Unions))
6. Fixed precision of 38 and scale of 9 ([doc](https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type))
7. All Scala types are encoded as big endian `ByteString` for Bigtable
8. Nested fields are encoded flat with field names joined with `.`, e.g. `level1.level2.level3`
9. Parquet logical types ([doc](https://github.com/apache/parquet-format/blob/master/LogicalTypes.md))
10. See [protobuf.md](https://github.com/spotify/magnolify/blob/master/docs/protobuf.md) for more
