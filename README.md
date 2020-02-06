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
- `magnolify-protobuf` - conversion between Scala types and [Google Protocol Buffer](https://developers.google.com/protocol-buffers/docs/overview) `Message`

# Usage

Cats, ScalaCheck, and Guava typeclass derivation can be performed both automatically and semi-automatically.

Automatic derivation are provided as implicits through `import magnolify.$module.auto._`.  It works with context bounds i.e. `def plus[T: Semigroup](x: T, y: T)` and implicit paramemters i.e. `def f[T](x: T, y: T)(implicit sg: Semigroup[T])`. The following examples summon them via `implicitly`.

```scala
case class Inner(int: Int, str: String)
case class Outer(inner: Inner)

// Cats Semigroup
import magnolify.cats.auto._
import cats._
import cats.instances.all._ // implicit instances for Semigroup[Int], etc.
val sg: = implicitly[Semigroup[Outer]]
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
val eq: Eq[Outer] = EqDerivation[Outer]
val hash: Hash[Outer] = HashDerivation[Outer]
val sg: Semigroup[Outer] = SemigroupDerivation[Outer]
val mon: Monoid[Outer] = MonoidDerivation[Outer]
// this fails due to missing `Group[String]` instance
val group: Group[Outer] = GroupDerivation[Outer]

import magnolify.scalacheck.semiauto._
import org.scalacheck._
val arb: Arbitrary[Outer] = ArbitraryDerivation[Outer]
val cogen: Cogen[Outer] = CogenDerivation[Outer]

import magnolify.guava.semiauto._
val fnl: Funnel[Outer] = FunnelDerivation[Outer]
```

Typeclasses for data type conversion must be called explicitly.

```scala
import java.net.URI
case class Inner(long: Long, str: String, uri: URI)
case class Outer(inner: Inner)
val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

// Avro GenericRecord
import magnolify.avro._
import org.apache.avro.generic.GenericRecord
implicit val uriField = AvroField.from[String](URI.create)(_.toString) // custom field type
val avroType = AvroType[Outer]
val genericRecord: GenericRecord = avroType.to(record)
val copy: Outer = avroType.from(genericRecord)

avroType.schema // Avro Schema

// BigQuery TableRow
import magnolify.bigquery._
import com.google.api.services.bigquery.model.TableRow
implicit val uriField = TableRowField.from[String](URI.create)(_.toString) // custom field type
val tableRowType = TableRowType[Outer]
val tableRow: TableRow = tableRowType.to(record)
val copy: Outer = tableRowType.from(tableRow)

tableRowType.schema // BigQuery TableSchema

// Datastore Entity
import magnolify.datastore._
implicit val uriField = EntityField.from[String](URI.create)(_.toString) // custom field type
val entityType = EntityType[Outer]
val entityBuilder: Entity.Builder = entityType.to(record)
val copy: Outer = entityType.from(entityBuilder.build)

// TensorFlow Example
import magnolify.tensorflow._
import com.google.protobuf.ByteString
// custom field types
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
implicit val uriField =
  ExampleField.from[ByteString](b => URI.create(b.toStringUtf8))(u => ByteString.copyFromUtf8(u.toString))
val exampleType = ExampleType[Outer]
val exampleBuilder: Example.Builder = exampleType.to(record)
val copy = exampleType.from(exampleBuilder.build)
```

Protobuf works slightly differently in that you need to specify both the type of the case class 
and of the Proto file. Note that Protobuf support has some limitations:
1. It doesn't support wrapping optional types in `scala.Option`,
 because the protobuf default value behavior makes it ambiguous whether options should be `None` 
 or have the default value. This means that round-trip behavior of an Option that was None would 
 be the default value. To avoid this, we have chosen not to implement an Option converter, so 
 case classes with Options will fail to compile. 
2. It doesn't support Map fields, as descriptors for those can't be retrieved from the generated 
code. 

```scala

// Given a .proto file, with generated Java code imported into scope, 
// and a proto of the form
// message CustomP3 {
//    string u = 1;
//    int64 d = 2;
//}

import java.time.Duration
import java.net.URI
import magnolify.protobuf._

implicit val pfUri: ProtobufField[URI] = 
    ProtobufField.from[URI, String](URI.create)(_.toString)
implicit val pfDuration: ProtobufField[Duration] =
    ProtobufField.from[Duration, Long](Duration.ofMillis)(_.toMillis)
case class Custom(u: URI, d: Duration)

val pt = ProtobufType[Custom, CustomP3]

val cc = Custom(URI.create("http://example.com"), Duration.ofMillis(0L))

val customProto = pt(cc) // is of type CustomP3, extends Message
val ccCopy = pt(customProto) // is a case class of type Custom

```

# License

Copyright 2019 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
