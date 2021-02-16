Type Mapping
============

| Scala                     | Avro                     | BigQuery               | Bigtable<sup>6</sup>            | Datastore             | Parquet                         | Protobuf               | TensorFlow          |
| ------------------------- | ------------------------ | ---------------------- | ------------------------------- | --------------------- | ------------------------------- | ---------------------- | ------------------- |
| `Unit`                    | `NULL`                   | -                      | -                               | `Null`                | -                               | -                      | -                   |
| `Boolean`                 | `BOOLEAN`                | `BOOL`                 | `Byte`                          | `Boolean`             | `BOOLEAN`                       | `Boolean`              | `INT64`<sup>2</sup> |
| `Char`                    | `INT`<sup>2</sup>        | `INT64`<sup>2</sup2>   | `Char`                          | `Integer`<sup>2</sup> | `INT32`<sup>2</sup>             | `Int`<sup>2</sup>      | `INT64`<sup>2</sup> |
| `Byte`                    | `INT`<sup>2</sup>        | `INT64`<sup>2</sup2>   | `Byte`                          | `Integer`<sup>2</sup> | `INT32`<sup>8</sup>             | `Int`<sup>2</sup>      | `INT64`<sup>2</sup> |
| `Short`                   | `INT`<sup>2</sup>        | `INT64`<sup>2</sup2>   | `Short`                         | `Integer`<sup>2</sup> | `INT32`<sup>8</sup>             | `Int`<sup>2</sup>      | `INT64`<sup>2</sup> |
| `Int`                     | `INT`                    | `INT64`<sup>2</sup2>   | `Int`                           | `Integer`<sup>2</sup> | `INT32`<sup>8</sup>             | `Int`                  | `INT64`<sup>2</sup> |
| `Long`                    | `LONG`                   | `INT64`                | `Long`                          | `Integer`             | `INT64`<sup>8</sup>             | `Long`                 | `INT64`             |
| `Float`                   | `FLOAT`                  | `FLOAT64`<sup>2</sup2> | `Float`                         | `Double`<sup>2</sup>  | `FLOAT`                         | `Float`                | `FLOAT`             |
| `Double`                  | `DOUBLE`                 | `FLOAT64`              | `Double`                        | `Double`              | `DOUBLE`                        | `Double`               | `FLOAT`<sup>2</sup> |
| `String`                  | `STRING`                 | `STRING`               | `String`                        | `String`              | `BINARY` + `STRING`<sup>8</sup> | `String`               | `BYTES`<sup>2</sup> |
| `Array[Byte]`             | `BYTES`                  | `BYTES`                | `ByteString`                    | `Blob`                | `BINARY`                        | `ByteString`           | `BYTES`             |
| `ByteString`              | -                        | -                      | `ByteString`                    | `Blob`                | -                               | `ByteString`           | `BYTES`             |
| Enum                      | `ENUM`                   | `STRING`<sup>2</sup2>  | `String`                        | `String`<sup>2</sup>  | `BINARY` + `ENUM`<sup>8</sup>   | Enum                   | `BYTES`<sup>2</sup> |
| `BigInt`                  | -                        | -                      | `BigInt`                        | -                     | -                               | -                      | -                   |
| `BigDecimal`              | `BYTES`<sup>3</sup>      | `NUMERIC`<sup>5</sup2> | `Int` scale + unscaled `BigInt` | -                     | Int/Binary/Fixed<sup>8</sup>    | -                      | -                   |
| `Option[T]`               | `UNION`<sup>4</sup>      | `NULLABLE`             | Empty as `None`                 | Absent as `None`      | `OPTIONAL`                      | `optional`<sup>9</sup> | Size <= 1           |
| `Iterable[T]`<sup>1</sup> | `ARRAY`                  | `REPEATED`             | -                               | `Array`               | `REPEATED`                      | `repeated`             | Size >= 0           |
| Nested                    | `RECORD`                 | `STRUCT`               | Flat<sup>7</sup>                | `Entity`              | Group                           | `Message`              | Flat<sup>7</sup>    |
| `Map[String, T]`          | `MAP`                    | -                      | -                               | -                     | -                               | -                      | -                   |
| `Instant`                 | `INT`<sup>3</sup>        | `TIMESTAMP`            | -                               | `Integer`             | `INT64`<sup>8</sup>             | -                      | -                   |
| `LocalDateTime`           | `LONG`<sup>3</sup>       | `DATETIME`             | -                               | -                     | `INT64`<sup>8</sup>             | -                      | -                   |
| `OffsetTime`              | -                        | -                      | -                               | -                     | `INT32`/`INT64`<sup>8</sup>     | -                      | -                   |
| `LocalTime`               | `INT`/`LONG`<sup>3</sup> | `TIME`                 | -                               | -                     | `INT32`/`INT64`<sup>8</sup>     | -                      | -                   |
| `LocalDate`               | `INT`<sup>3</sup>        | `DATE`                 | -                               | -                     | `INT32`<sup>8</sup>             | -                      | -                   |
| `UUID`                    | `STRING`<sup>3</sup>     | -                      | 16 bytes                        | -                     | Fixed<sup>8</sup>               | -                      | -                   |

1. Any subtype of `Iterable[T]`
2. Unsafe conversions, `import magnolify.$MODULE.unsafe._`
3. Avro logical types ([doc](https://avro.apache.org/docs/current/spec.html#Logical+Types))
4. `UNION` of `[NULL, T]` and defaults to `NULL` ([doc](https://avro.apache.org/docs/current/spec.html#Unions))
5. Fixed precision of 38 and scale of 9 ([doc](https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type))
6. All Scala types are encoded as big endian `ByteString` for Bigtable
7. Nested fields are encoded flat with field names joined with `.`, e.g. `level1.level2.level3`
8. Parquet logical types ([doc](https://github.com/apache/parquet-format/blob/master/LogicalTypes.md))
9. See [protobuf.md](https://github.com/spotify/magnolify/blob/master/docs/protobuf.md) for more
