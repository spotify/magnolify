name := "magnolia-data"
description := "Magnolia add-on modules for data"

val magnoliaVersion = "0.11.0"

val avroVersion = "1.9.1"
val bigqueryVersion = "v2-rev20181104-1.27.0"
val catsVersion = "2.0.0"
val datastoreVersion = "1.6.0"
val jacksonVersion = "2.10.0"
val jodaTimeVersion = "2.10.4"
val protobufVersion = "3.10.0"
val scalacheckVersion = "1.14.2"
val scalatestVersion = "3.0.8"
val tensorflowVersion = "1.14.0"

val commonSettings = Seq(
  organization := "me.lyh",

  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),

  libraryDependencies += {
    if (scalaBinaryVersion.value == "2.11") {
      "me.lyh" %% "magnolia" % "0.10.1-jto"
    } else {
      "com.propensive" %% "magnolia" % magnoliaVersion
    }
  },

  // protobuf-lite is an older subset of protobuf-java and causes issues
  excludeDependencies += "com.google.protobuf" % "protobuf-lite",

  // Release settings
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  sonatypeProfileName           := "me.lyh",

  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/nevillelyh/magnolia-data")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/nevillelyh/magnolia-data.git"),
    "scm:git:git@github.com:nevillelyh/magnolia-data.git")),
  developers := List(
    Developer(id="sinisa_lyh", name="Neville Li", email="neville.lyh@gmail.com", url=url("https://twitter.com/sinisa_lyh")),
    Developer(id="andrewsmartin", name="Andrew Martin", email="andrewsmartin.mg@gmail.com", url=url("https://twitter.com/andrew_martin92")),
    Developer(id="daikeshi", name="Keshi Dai", email="keshi.dai@gmail.com", url=url("https://twitter.com/daikeshi"))
  )
)

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = project.in(file(".")).settings(
  commonSettings ++ noPublishSettings
).aggregate(
  scalacheck,
  cats,
  diffy,
  avro,
  bigquery,
  datastore,
  tensorflow,
  test
)

// shared code for unit tests
lazy val test: Project = project.in(file("test")).settings(
  commonSettings ++ noPublishSettings,
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
    // Custom types
    "com.google.protobuf" % "protobuf-java" % protobufVersion % Test,
    "joda-time" % "joda-time" % jodaTimeVersion % Test
  )
)

lazy val scalacheck: Project = project.in(file("scalacheck")).settings(
  moduleName := "magnolia-data-scalacheck",
  commonSettings,
  description := "Magnolia add-on for ScalaCheck",
  libraryDependencies += "org.scalacheck" %% "scalacheck" % scalacheckVersion,
  // For testing derived Gen[T] instances
  libraryDependencies += "org.scalatest" %% "scalatest" % scalatestVersion % Test,
).dependsOn(
  test % "test->test"
)

lazy val cats: Project = project.in(file("cats")).settings(
  moduleName := "magnolia-data-cats",
  commonSettings,
  description := "Magnolia add-on for Cats",
  libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)

lazy val diffy: Project = project.in(file("diffy")).settings(
  moduleName := "magnolia-data-diffy",
  commonSettings,
  description := "Magnolia add-on for diffing data"
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)

lazy val avro: Project = project.in(file("avro")).settings(
  moduleName := "magnolia-data-avro",
  commonSettings,
  description := "Magnolia add-on for Apache Avro",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.apache.avro" % "avro" % avroVersion % Provided
  )
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)

lazy val bigquery: Project = project.in(file("bigquery")).settings(
  moduleName := "magnolia-data-bigquery",
  commonSettings,
  description := "Magnolia add-on for Google Cloud BigQuery",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Provided,
    "joda-time" % "joda-time" % jodaTimeVersion % Provided,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % Test
  )
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)

lazy val datastore: Project = project.in(file("datastore")).settings(
  commonSettings,
  moduleName := "magnolia-data-datastore",
  description := "Magnolia add-on for Google Cloud Datastore",
  libraryDependencies ++= Seq(
    "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Provided,
    "joda-time" % "joda-time" % jodaTimeVersion % Provided
  )
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)

lazy val tensorflow: Project = project.in(file("tensorflow")).settings(
  moduleName := "magnolia-data-tensorflow",
  commonSettings,
  description := "Magnolia add-on for TensorFlow",
  libraryDependencies ++= Seq(
    "org.tensorflow" % "proto" % tensorflowVersion % Provided
  )
).dependsOn(
  scalacheck % Test,
  test % "test->test"
)
