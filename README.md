magnolify
=========

[![Build Status](https://github.com/spotify/magnolify/actions/workflows/ci.yml/badge.svg)](https://github.com/spotify/magnolify/actions/workflows/ci.yml)
[![codecov.io](https://codecov.io/github/spotify/magnolify/coverage.svg?branch=master)](https://codecov.io/github/spotify/magnolify?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/magnolify-shared_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/magnolify-shared_2.13)
[![Scaladoc](https://img.shields.io/badge/scaladoc-latest-blue.svg)](https://spotify.github.io/magnolify/api/magnolify/index.html)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A collection of [Magnolia](https://github.com/propensive/magnolia) add-ons for common type class derivation, data type conversion, etc.; a simpler and faster successor to [shapeless-datatype](https://github.com/nevillelyh/shapeless-datatype).

# Modules

This library includes the following modules.

* `magnolify-avro` - conversion between Scala types and [Apache Avro](https://github.com/apache/avro) `GenericRecord`
* `magnolify-bigquery` - conversion between Scala types and [Google Cloud BigQuery](https://cloud.google.com/bigquery/) `TableRow`
* `magnolify-bigtable` - conversion between Scala types and [Google Cloud Bigtable](https://cloud.google.com/bigtable) to `Mutation`, from `Row`
* `magnolify-cats` - type class derivation for [Cats](https://github.com/typelevel/cats), specifically
  * [`Eq[T]`](https://typelevel.org/cats/api/cats/kernel/Eq.html)
  * [`Hash[T]`](https://typelevel.org/cats/api/cats/kernel/Hash.html)
  * [`Semigroup[T]`](https://typelevel.org/cats/api/cats/kernel/Semigroup.html), [`CommutativeSemigroup[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeSemigroup.html), [`Band[T]`](https://typelevel.org/cats/api/cats/kernel/Band.html)
  * [`Monoid[T]`](https://typelevel.org/cats/api/cats/kernel/Monoid.html), [`CommutativeMonoid[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeMonoid.html)
  * [`Group[T]`](https://typelevel.org/cats/api/cats/kernel/Group.html), [`CommutativeGroup[T]`](https://typelevel.org/cats/api/cats/kernel/CommutativeGroup.html)
* `magnolify-datastore` - conversion between Scala types and [Google Cloud Datastore](https://cloud.google.com/datastore/) `Entity`
* `magnolify-guava` - type class derivation for [Guava](https://guava.dev)
  * [`Funnel[T]`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/hash/Funnel.html)
* `magnolify-neo4j` - conversion between Scala types and [Value](https://neo4j.com/docs/driver-manual/1.7/cypher-values/)
* `magnolify-parquet` - support for [Parquet](http://parquet.apache.org/) columnar storage format.
* `magnolify-protobuf` - conversion between Scala types and [Google Protocol Buffer](https://developers.google.com/protocol-buffers/docs/overview) `Message`
* `magnolify-refined` - support for simple refinement types from [Refined](https://github.com/fthomas/refined).
* `magnolify-scalacheck` - type class derivation for [ScalaCheck](https://github.com/typelevel/scalacheck)
  * [`Arbitrary[T]`](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#universally-quantified-properties)
  * [`Cogen[T]`](https://github.com/typelevel/scalacheck/blob/master/src/main/scala/org/scalacheck/Cogen.scala)
* `magnolify-tensorflow` - conversion between Scala types and [TensorFlow](https://www.tensorflow.org/) `Example`

# Usage

See [derivation.md](https://github.com/spotify/magnolify/tree/master/docs/derivation.md) for type class derivation for Cats, Scalacheck, and Guava.

See [avro.md](https://github.com/spotify/magnolify/tree/master/docs/avro.md)
[bigquery.md](https://github.com/spotify/magnolify/tree/master/docs/bigquery.md)
[bigtable.md](https://github.com/spotify/magnolify/tree/master/docs/bigtable.md)
[datastore.md](https://github.com/spotify/magnolify/tree/master/docs/datastore.md)
[protobuf.md](https://github.com/spotify/magnolify/tree/master/docs/protobuf.md)
[tensorflow.md](https://github.com/spotify/magnolify/tree/master/docs/tensorflow.md) for data type conversions for these libraries.
See [parquet.md](https://github.com/spotify/magnolify/tree/master/docs/parquet.md) for Parquet IO support. Also see [enums.md](https://github.com/spotify/magnolify/tree/master/docs/enums.md) for enum types and [refined.md](https://github.com/spotify/magnolify/tree/master/docs/derivation.md) for refinement types support.
Finally, see [mapping.md](https://github.com/spotify/magnolify/blob/master/docs/mapping.md) for a mapping table of Scala types supported by conversion and IO modules.

# How to Release

Magnolify automates releases using [sbt-ci-release](https://github.com/sbt/sbt-ci-release) with Github Actions. Simply push a new tag:

```shell
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

Note that the tag version MUST start with `v` to be picked up as the release version.

# License

Copyright 2019-2023 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
