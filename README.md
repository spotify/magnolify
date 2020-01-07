magnolify
=========

[![Build Status](https://travis-ci.org/spotify/magnolify.svg?branch=master)](https://travis-ci.org/spotify/magnolify)
[![codecov.io](https://codecov.io/github/spotify/magnolify/coverage.svg?branch=master)](https://codecov.io/github/spotify/magnolify?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/magnolify.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/magnolify-shared_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/magnolify-shared_2.13)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A collection of typeclass derivations for conversions between case classes and other wrapper types, created with [Magnolia](https://github.com/propensive/magnolia); a simpler and faster successor to [shapeless-datatype](https://github.com/nevillelyh/shapeless-datatype).

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
  - [`Cogen[T]`](https://github.com/typelevel/scalacheck/blob/master/src/main/scala/org/scalacheck/Cogen.scala#L20-L29)
- `magnolify-guava` - type class derivation for [Guava](https://guava.dev)
  - [`Funnel[T]`](https://guava.dev/releases/25.0-jre/api/docs/com/google/common/hash/Funnel.html)
  
- `magnolify-avro` - conversion between Scala case classes and [Apache Avro](https://github.com/apache/avro) `GenericRecord`
- `magnolify-bigquery` - conversion between Scala case classes and [Google Cloud BigQuery](https://cloud.google.com/bigquery/) `TableRow`
- `magnolify-datastore` - conversion between Scala case classes and [Google Cloud Datastore](https://cloud.google.com/datastore/) `Entity`
- `magnolify-tensorflow` - conversion between Scala case classes and [TensorFlow](https://www.tensorflow.org/) `Example`

# Usage

Cats, ScalaCheck, and Guava type class derivation can be performed both automatically and semi-automatically.

Automatic derivation with implicits:
```scala
case class MyCaseClass(field: NestedCaseClass) 
case class NestedCaseClass(innerField: String) // works with other non-String elements, this is just an example

// implicitly keyword summons an instance of the typeclass wrapping a given case class
// typeclasses are generated at compile time with macros & Magnolia
// this works if each element within the case class has a typeclass instance defined within the implicit scope

// an example with Cats using the Eq typeclass
import magnolify.cats.auto._
import cats.kernel.Eq
// similar to Eq.fromUniversalEquals for a case class but it's at compile time, and don't need == defined reasonably
// instead of using == in fromUniversalEquals, you need an Eq instance for each part of your case class
implicit val eqString: Eq[String] = ??? // could be fromUniversalEquals, but doesn't have to be
val eqCaseClass: Eq[MyCaseClass] = implicitly[Eq[MyCaseClass]] 

// an example with ScalaCheck using the Arbitrary typeclass
// this example is a bit more practical, since there's no typeclass instantiation built into Scalacheck, unlike Cats
import magnolify.scalacheck.auto._
import org.scalacheck.Arbitrary
implicit val arbString: Arbitrary[String] = ??? // could be Arbitrary.arbitrary[String]
val arbitraryCaseClass: Arbitrary[MyCaseClass] = implicitly[Arbitrary[MyCaseClass]]

// an example with Guava using the Funnel typeclass
import magnolify.guava.auto._
import com.google.common.hash.Funnel
implicit val funnelString: Funnel[String] = ??? // could be Funnels.stringFunnel(charset)
val implicitly[Funnel[MyCaseClass]]
```

Semi-automatic derivation happens explicitly by calling the derivation yourself instead of summoning an implicit.
```scala
import magnolify.cats.semiauto._
EqDerivation[MyCaseClass]
HashDerivation[MyCaseClass]
SemigroupDerivation[MyCaseClass]
MonoidDerivation[MyCaseClass]
GroupDerivation[MyCaseClass]

import magnolify.scalacheck.semiauto._
ArbitraryDerivation[MyCaseClass]
CogenDerivation[MyCaseClass]

import magnolify.guava.semiauto._
FunnelDerivation[MyCaseClass]
```

Case class type conversion must be called explicitly, specifying the case class used for conversion, rather than deriving entirely with implicits.

```scala
// an annotated example with Avro GenericRecord
import magnolify.avro._
import org.apache.avro.generic.GenericRecord
val typeclass = AvroType[MyCaseClass] // AvroType typeclass instance defines to and from conversions between Case Class and GenericRecord
val genericRecord: GenericRecord = typeclass.to(MyCaseClass(...)) // instantiate a case class, then pass in
val caseClass: MyCaseClass = typeclass.from(genericRecord) // roundtrip

typeclass.schema // Avro Schema
implicit val uriField = AvroField.from[String](URI.create)(_.toString) // custom field type

// The other three converters work similarly to Avro: 

// an example with BigQuery TableRow
import magnolify.bigquery._
val typeclass = TableRowType[MyCaseClass] 
val tableRow: TableRow = typeclass.to(MyCaseClass(...)) 
val caseClass: MyCaseClass = typeclass.from(tableRow)

typeclass.schema // BigQuery TableSchema
implicit val uriField = TableRowField.from[String](URI.create)(_.toString) // custom field type

// an example with Datastore Entity
import magnolify.datastore._
val t = EntityType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(r)

implicit val uriField = EntityField.from[String](URI.create)(_.toString) // custom field type

// an example with TensorFlow TFExample
import magnolify.tensorflow._
val t = ExampleType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(r)

// custom field type
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8) 
```

# License

Copyright 2019 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
