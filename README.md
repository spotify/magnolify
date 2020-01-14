magnolify
=========

[![Build Status](https://travis-ci.org/spotify/magnolify.svg?branch=master)](https://travis-ci.org/spotify/magnolify)
[![codecov.io](https://codecov.io/github/spotify/magnolify/coverage.svg?branch=master)](https://codecov.io/github/spotify/magnolify?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/magnolify.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/magnolify-shared_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/magnolify-shared_2.13)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A collection of [Magnolia](https://github.com/propensive/magnolia) add-ons for common typeclass derivation, data type conversion, etc.; a simpler and faster successor to [shapeless-datatype](https://github.com/nevillelyh/shapeless-datatype).

# Modules

This library includes the following modules.

- `magnolify-cats` - type class derivation for [Cats](https://github.com/typelevel/cats), specifically
  - [`Eq[T]`](https://typelevel.org/cats/api/cats/kernel/Eq.html)
  - [`Hash[T]`](https://typelevel.org/cats/api/cats/kernel/Hash.html)
  - [`Semigroup[T]`](https://typelevel.org/cats/api/cats/kernel/Semigroup.html)
  - [`Monoid[T]`](https://typelevel.org/cats/api/cats/kernel/Monoid.html)
  - [`Group[T]`](https://typelevel.org/cats/api/cats/kernel/Group.html)
- `magnolify-scalacheck` - type class derivation for [ScalaCheck](https://github.com/typelevel/scalacheck)
  - [`Arbitrary[T]`](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#universally-quantified-properties)
  - [`Cogen[T]`](https://github.com/typelevel/scalacheck/blob/master/src/main/scala/org/scalacheck/Cogen.scala)
- `magnolify-guava` - type class derivation for [Guava](https://guava.dev)
  - [`Funnel[T]`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/hash/Funnel.html)

- `magnolify-avro` - conversion between Scala types and [Apache Avro](https://github.com/apache/avro) `GenericRecord`
- `magnolify-bigquery` - conversion between Scala types and [Google Cloud BigQuery](https://cloud.google.com/bigquery/) `TableRow`
- `magnolify-datastore` - conversion between Scala types and [Google Cloud Datastore](https://cloud.google.com/datastore/) `Entity`
- `magnolify-tensorflow` - conversion between Scala types and [TensorFlow](https://www.tensorflow.org/) `Example`

# Usage

Cats, ScalaCheck, and Guava type class derivation can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.$module.auto._`.  It works with context bounds i.e. `def plus[T: Semigroup](x: T, y: T)` and implicit paramemters i.e.
`def f[T](x: T, y: T)(implicit sg: Semigroup[T])`. The following examples summon them via `implicitly`.

```scala
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Cats Semigroup
import magnolify.cats.auto._
import cats._
import cats.instances.all._ // implicit instances for Semigroup[Int], etc.
val sg: Semigroup[Outer] = implicitly[Semigroup[Outer]]
sg.combine(Outer(Inner(1, "hello, ")), Outer(Inner(100, "world!")))
// = Outer(Inner(101,hello, world!))

// ScalaCheck Arbitrary
import magnolify.scalacheck.auto._
import org.scalacheck._ // implicit instances for Arbitrary[Int], etc.
val arb: Arbitrary[Outer] = implicitly[Arbitrary[Outer]]
arb.arbitrary.sample
// = Some(Outer(Inter(12345, abcde)))

// Guava Funnel
import magnolify.guava.auto._ // includes implicit instances for Funnel[Int], etc.
import com.google.common.hash._
val fnl: Funnel[Outer] = implicitly[Funnel[Outer]]
val bf: BloomFilter[Outer] = BloomFilter.create[Outer](fnl, 1000)
```

Semi-automatic derivation needs to be called explicitly.

```scala
import magnolify.cats.semiauto._
import cats._
import cats.instances.all._
EqDerivation[Outer]
HashDerivation[Outer]
SemigroupDerivation[Outer]
MonoidDerivation[Outer]
// this fails due to missing `Group[String]` instance
// GroupDerivation[Outer]

import magnolify.scalacheck.semiauto._
import org.scalacheck._
ArbitraryDerivation[Outer]
CogenDerivation[Outer]

import magnolify.guava.semiauto._
FunnelDerivation[Outer]
```

Typeclasses for data type conversion must be called explicitly.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)

// Avro GenericRecord
import magnolify.avro._
import org.apache.avro.generic.GenericRecord
implicit val uriField = AvroField.from[String](URI.create)(_.toString) // custom field type
val avroType = AvroType[Outer]
val genericRecord: GenericRecord = avroType.to(Outer(Inner(1L, "hello", URI.create("https://www.spotify.com"))))
val caseClass: Outer = avroType.from(genericRecord)

avroType.schema // Avro Schema

// BigQuery TableRow
import magnolify.bigquery._
import com.google.api.services.bigquery.model.TableRow
implicit val uriField = TableRowField.from[String](URI.create)(_.toString) // custom field type
val tableRowType = TableRowType[Outer]
val tableRow: TableRow = tableRowType.to(Outer(Inner(1L, "hello", URI.create("https://www.spotify.com"))))
val caseClass: Outer = tableRowType.from(tableRow)

tableRowType.schema // BigQuery TableSchema

// Datastore Entity
import magnolify.datastore._
implicit val uriField = EntityField.from[String](URI.create)(_.toString) // custom field type
val entityType = EntityType[Outer]
val entity = entityType.to(Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))).build
val caseClass: Outer = entityType.from(entity)

// TensorFlow Example
import magnolify.tensorflow._
import com.google.protobuf.ByteString
// custom field types
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
implicit val uriField =
  ExampleField.from[ByteString](b => URI.create(b.toStringUtf8))(u => ByteString.copyFromUtf8(u.toString))
val exampleType = ExampleType[Outer]
val example = exampleType.to(Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))).build
val caseClass = exampleType.from(example)
```

# License

Copyright 2019 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
