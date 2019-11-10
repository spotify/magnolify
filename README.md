magnolify
=========

[![Build Status](https://travis-ci.org/spotify/magnolify.svg?branch=master)](https://travis-ci.org/spotify/magnolify)
[![codecov.io](https://codecov.io/github/spotify/magnolify/coverage.svg?branch=master)](https://codecov.io/github/spotify/magnolify?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/magnolify.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/magnolify-shared_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/magnolify-shared_2.13)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A collection of [Magnolia](https://github.com/propensive/magnolia) add-on modules, a simpler and faster successor to [shapeless-datatype](https://github.com/nevillelyh/shapeless-datatype).

# Modules

This library includes the following modules.

- `magnolify-cats` - type class derivation for [Cats](https://github.com/typelevel/cats) `Eq[T]`, `Semigroup[T]`, `Monoid[T]`, `Group[T]`, etc.
- `magnolify-scalacheck` - type class derivation for [ScalaCheck](https://github.com/typelevel/scalacheck) `Arbitrary[T]`, `Cogen[T]`, etc.
- `magnolify-avro` - conversion between Scala case classes and [Apache Avro](https://github.com/apache/avro) `GenericRecord`
- `magnolify-bigquery` - conversion between Scala case classes and [Google Cloud BigQuery](https://cloud.google.com/bigquery/) `TableRow`
- `magnolify-datastore` - conversion between Scala case classes and [Google Cloud Datastore](https://cloud.google.com/datastore/) `Entity`
- `magnolify-tensorflow` - conversion between Scala case classes and [TensorFlow](https://www.tensorflow.org/) `Example`

# Usage

Cats and ScalaCheck type class derivation can be performed both automatically and semi-automatically.

```scala
import magnolify.cats.auto._
implicitly[Eq[MyCaseClass]]

import magnolify.scalacheck.auto._
implicitly[Arbitrary[MyCaseClass]]
implicitly[Cogen[MyCaseClass]]
```

```scala
import magnolify.cats.semiauto._
EqDerivation[MyCaseClass]
SemigroupDerivation[MyCaseClass]
MonoidDerivation[MyCaseClass]
GroupDerivation[MyCaseClass]

import magnolify.scalacheck.semiauto._
ArbitraryDerivation[MyCaseClass]
CogenDerivation[MyCaseClass]
```

Case class type conversion must be done explicitly.

```scala
import magnolify.avro._
val t = AvroType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(t)

t.schema // Avro Schema
implicit val uriField = AvroField.from[String](URI.create)(_.toString) // custom field type
```

```scala
import magnolify.bigquery._
val t = TableRowType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(t)

t.schema // BigQuery TableSchema
implicit val uriField = TableRowField.from[String](URI.create)(_.toString) // custom field type
```

```scala
import magnolify.datastore._
val t = EntityType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(t)

implicit val uriField = EntityField.from[String](URI.create)(_.toString) // custom field type
```

```scala
import magnolify.tensorflow._
val t = ExampleType[MyCaseClass]
val r = t.to(MyCaseClass(...))
val c = t.from(t)

// custom field type
implicit val stringField = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8) 
```

# License

Copyright 2019 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
