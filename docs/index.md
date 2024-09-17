# Magnolify

A collection of [Magnolia](https://github.com/propensive/magnolia) add-ons for common type class derivation, data type conversion, etc.; a simpler and faster successor to [shapeless-datatype](https://github.com/nevillelyh/shapeless-datatype).

## Getting help

- [GitHub discussions](https://github.com/spotify/magnolify/discussions)

# Modules

This library includes the following modules.

- @ref:[`magnolify-avro`](avro.md) - conversion between Scala types and [Apache Avro](https://github.com/apache/avro) `GenericRecord`
- @ref:[`magnolify-beam`](beam.md) - conversion between Scala types and [Apache Beam](https://beam.apache.org/) [schema types](https://beam.apache.org/documentation/programming-guide/#schemas)
- @ref:[`magnolify-bigquery`](bigquery.md) - conversion between Scala types and [Google Cloud BigQuery](https://cloud.google.com/bigquery/) `TableRow`
- @ref:[`magnolify-bigtable`](bigtable.md) - conversion between Scala types and [Google Cloud Bigtable](https://cloud.google.com/bigtable) to `Mutation`, from `Row`
- @ref:[`magnolify-cats`](cats.md) - type class derivation for [Cats](https://github.com/typelevel/cats), specifically
    - [`Eq[T]`](https://typelevel.org/cats/api/cats/kernel/Eq.html)
    - [`Hash[T]`](https://typelevel.org/cats/api/cats/kernel/Hash.html)
    - [`Semigroup[T]`](https://typelevel.org/cats/api/cats/kernel/Semigroup.html), [`CommutativeSemigroup[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeSemigroup.html), [`Band[T]`](https://typelevel.org/cats/api/cats/kernel/Band.html)
    - [`Monoid[T]`](https://typelevel.org/cats/api/cats/kernel/Monoid.html), [`CommutativeMonoid[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeMonoid.html)
    - [`Group[T]`](https://typelevel.org/cats/api/cats/kernel/Group.html), [`CommutativeGroup[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeGroup.html)
- @ref:[`magnolify-datastore`](datastore.md) - conversion between Scala types and [Google Cloud Datastore](https://cloud.google.com/datastore/) `Entity`
- @ref:[`magnolify-guava`](guava.md) - type class derivation for [Guava](https://guava.dev)
    - [`Funnel[T]`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/hash/Funnel.html)
- @ref:[`magnolify-neo4j`](neo4j.md) - conversion between Scala types and [Value](https://neo4j.com/docs/driver-manual/1.7/cypher-values/)
- @ref:[`magnolify-parquet`](parquet.md) - support for [Parquet](http://parquet.apache.org/) columnar storage format.
- @ref:[`magnolify-protobuf`](protobuf.md) - conversion between Scala types and [Google Protocol Buffer](https://developers.google.com/protocol-buffers/docs/overview) `Message`
- @ref:[`magnolify-refined`](refined.md) - support for simple refinement types from [Refined](https://github.com/fthomas/refined).
- @ref:[`magnolify-scalacheck`](scalacheck.md) - type class derivation for [ScalaCheck](https://github.com/typelevel/scalacheck)
    - [`Arbitrary[T]`](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#universally-quantified-properties)
    - [`Cogen[T]`](https://github.com/typelevel/scalacheck/blob/master/src/main/scala/org/scalacheck/Cogen.scala)
- @ref:[`magnolify-tensorflow`](tensorflow.md) - conversion between Scala types and [TensorFlow](https://www.tensorflow.org/) `Example`

Complete type mapping @ref:[here](mapping.md).

@@@ index
- @ref:[Avro](avro.md)
- @ref:[Beam](beam.md)
- @ref:[BigQuery](bigquery.md)
- @ref:[Bigtable](bigtable.md)
- @ref:[Cats](cats.md)
- @ref:[Datastore](datastore.md)
- @ref:[Guava](guava.md)
- @ref:[Neo4j](neo4j.md)
- @ref:[Parquet](parquet.md)
- @ref:[Protobuf](protobuf.md)
- @ref:[Refined](refined.md)
- @ref:[ScalaCheck](scalacheck.md)
- @ref:[TensorFlow](tensorflow.md)
- @ref:[EnumType](enums.md)
- @ref:[Mapping](mapping.md)
- @ref:[Scaladoc](scaladoc.md)
@@@