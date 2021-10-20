/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
name := "magnolify"
description := "A collection of Magnolia add-on modules"

val magnoliaVersion = "1.0.0-M4"

val algebirdVersion = "0.13.8"
val avroVersion = Option(sys.props("avro.version")).getOrElse("1.10.2")
val bigqueryVersion = "v2-rev20210910-1.32.1"
val bigtableVersion = "2.2.0"
val catsVersion = "2.6.1"
val datastoreVersion = "2.1.2"
val guavaVersion = "30.1.1-jre"
val hadoopVersion = "3.3.1"
val jacksonVersion = "2.12.5"
val munitVersion = "0.7.28"
val paigesVersion = "0.4.2"
val parquetVersion = "1.12.0"
val protobufVersion = "3.18.0"
val refinedVersion = "0.9.17"
val scalacheckVersion = "1.15.4"
val shapelessVersion = "2.3.7"
val tensorflowVersion = "0.3.2"

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.13.6",
  crossScalaVersions := Seq("2.12.14", "2.13.6"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  scalacOptions ++= (scalaBinaryVersion.value match {
    case "2.12" => Seq("-language:higherKinds")
    case "2.13" => Nil
  }),
  libraryDependencies ++= Seq(
    "com.softwaremill.magnolia" %% "magnolia-core" % magnoliaVersion,
    "com.chuusai" %% "shapeless" % shapelessVersion,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  ),
  // https://github.com/typelevel/scalacheck/pull/427#issuecomment-424330310
  // FIXME: workaround for Java serialization issues
  Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
  // Release settings
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging
  ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  sonatypeProfileName := "com.spotify",
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/spotify/magnolify")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/spotify/magnolify.git"),
      "scm:git:git@github.com:spotify/magnolify.git"
    )
  ),
  developers := List(
    Developer(
      id = "sinisa_lyh",
      name = "Neville Li",
      email = "neville.lyh@gmail.com",
      url = url("https://twitter.com/sinisa_lyh")
    ),
    Developer(
      id = "andrewsmartin",
      name = "Andrew Martin",
      email = "andrewsmartin.mg@gmail.com",
      url = url("https://twitter.com/andrew_martin92")
    ),
    Developer(
      id = "daikeshi",
      name = "Keshi Dai",
      email = "keshi.dai@gmail.com",
      url = url("https://twitter.com/daikeshi")
    ),
    Developer(
      id = "clairemcginty",
      name = "Claire McGinty",
      email = "clairem@spotify.com",
      url = url("http://github.com/clairemcginty")
    ),
    Developer(
      id = "anne-decusatis",
      name = "Anne DeCusatis",
      email = "anned@spotify.com",
      url = url("http://twitter.com/precisememory")
    ),
    Developer(
      id = "stormy-ua",
      name = "Kirill Panarin",
      email = "kirill.panarin@gmail.com",
      url = url("https://twitter.com/panarin_kirill")
    ),
    Developer(
      id = "syodage",
      name = "Shameera Rathnayaka Yodage",
      email = "shameerayodage@gmail.com",
      url = url("https://twitter.com/syodage")
    )
  )
)

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = project
  .in(file("."))
  .settings(
    commonSettings ++ noPublishSettings
  )
  .aggregate(
    shared,
    scalacheck,
    cats,
    guava,
    avro,
    bigquery,
    bigtable,
    datastore,
    parquet,
    protobuf,
    tensorflow,
    tools,
    test
  )

lazy val shared: Project = project
  .in(file("shared"))
  .settings(
    commonSettings,
    moduleName := "magnolify-shared",
    description := "Shared code for Magnolify"
  )

// shared code for unit tests
lazy val test: Project = project
  .in(file("test"))
  .settings(
    commonSettings ++ noPublishSettings,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %% "cats-core" % catsVersion % Test
    ),
    ProtobufConfig / protobufRunProtoc := (args =>
      com.github.os72.protocjar.Protoc.runProtoc(args.toArray)
    )
  )
  .dependsOn(shared)
  .enablePlugins(ProtobufPlugin)

lazy val scalacheck: Project = project
  .in(file("scalacheck"))
  .settings(
    commonSettings,
    moduleName := "magnolify-scalacheck",
    description := "Magnolia add-on for ScalaCheck",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % scalacheckVersion
  )
  .dependsOn(
    shared,
    test % "test->test"
  )

lazy val cats: Project = project
  .in(file("cats"))
  .settings(
    commonSettings,
    moduleName := "magnolify-cats",
    description := "Magnolia add-on for Cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-laws" % catsVersion % Test,
      "com.twitter" %% "algebird-core" % algebirdVersion % Test
    )
  )
  .dependsOn(
    shared,
    scalacheck % Test,
    test % "test->test"
  )

lazy val guava: Project = project
  .in(file("guava"))
  .settings(
    commonSettings,
    moduleName := "magnolify-guava",
    description := "Magnolia add-on for Guava",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % guavaVersion % Provided
    )
  )
  .dependsOn(
    shared,
    scalacheck % Test,
    test % "test->test"
  )

lazy val refined: Project = project
  .in(file("refined"))
  .settings(
    commonSettings,
    moduleName := "magnolify-refined",
    description := "Magnolia add-on for Refined",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % guavaVersion % Provided,
      "eu.timepit" %% "refined" % refinedVersion % Provided,
      "org.scalameta" %% "munit" % munitVersion % Test,
      "org.apache.avro" % "avro" % avroVersion % Test,
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Test,
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Test,
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Test,
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Test
    )
  )
  .dependsOn(
    guava % "provided,test->test",
    avro % Provided,
    bigquery % Provided,
    bigtable % Provided,
    datastore % Provided,
    protobuf % Provided,
    tensorflow % Provided,
    test % "test->test"
  )

lazy val avro: Project = project
  .in(file("avro"))
  .settings(
    commonSettings,
    moduleName := "magnolify-avro",
    description := "Magnolia add-on for Apache Avro",
    libraryDependencies ++= Seq(
      "org.apache.avro" % "avro" % avroVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val bigquery: Project = project
  .in(file("bigquery"))
  .settings(
    commonSettings,
    moduleName := "magnolify-bigquery",
    description := "Magnolia add-on for Google Cloud BigQuery",
    libraryDependencies ++= Seq(
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Provided,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % Test
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val bigtable: Project = project
  .in(file("bigtable"))
  .settings(
    commonSettings,
    moduleName := "magnolify-bigtable",
    description := "Magnolia add-on for Google Cloud Bigtable",
    libraryDependencies ++= Seq(
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val datastore: Project = project
  .in(file("datastore"))
  .settings(
    commonSettings,
    moduleName := "magnolify-datastore",
    description := "Magnolia add-on for Google Cloud Datastore",
    libraryDependencies ++= Seq(
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val parquet: Project = project
  .in(file("parquet"))
  .settings(
    commonSettings,
    moduleName := "magnolify-parquet",
    description := "Magnolia add-on for Apache Parquet",
    libraryDependencies ++= Seq(
      "org.apache.parquet" % "parquet-avro" % parquetVersion % Provided,
      "org.apache.parquet" % "parquet-hadoop" % parquetVersion % Provided,
      "org.apache.hadoop" % "hadoop-client" % hadoopVersion % Provided
    )
  )
  .dependsOn(
    shared,
    avro % Test,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val protobuf: Project = project
  .in(file("protobuf"))
  .settings(
    commonSettings,
    moduleName := "magnolify-protobuf",
    description := "Magnolia add-on for Google Protocol Buffer",
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % protobufVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val tensorflow: Project = project
  .in(file("tensorflow"))
  .settings(
    commonSettings,
    moduleName := "magnolify-tensorflow",
    description := "Magnolia add-on for TensorFlow",
    libraryDependencies ++= Seq(
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val tools: Project = project
  .in(file("tools"))
  .settings(
    commonSettings,
    moduleName := "magnolify-tools",
    description := "Magnolia add-on for code generation",
    libraryDependencies ++= Seq(
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion,
      "org.apache.avro" % "avro" % avroVersion,
      "org.apache.parquet" % "parquet-hadoop" % parquetVersion,
      "org.typelevel" %% "paiges-core" % paigesVersion
    )
  )
  .dependsOn(
    shared,
    avro % Test,
    bigquery % Test,
    parquet % Test,
    test % "test->test"
  )

lazy val jmh: Project = project
  .in(file("jmh"))
  .settings(
    commonSettings,
    Jmh / classDirectory := (Test / classDirectory).value,
    Jmh / dependencyClasspath := (Test / dependencyClasspath).value,
    // rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile'
    // (otherwise a clean 'jmh:run' would fail)
    Jmh / compile := (Jmh / compile).dependsOn(Test / compile).value,
    Jmh / run := (Jmh / run).dependsOn(Jmh / compile).evaluated,
    libraryDependencies ++= Seq(
      "org.apache.avro" % "avro" % avroVersion % Test,
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Test,
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Test,
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Test,
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Test
    )
  )
  .dependsOn(
    scalacheck % Test,
    cats % Test,
    guava % Test,
    avro % Test,
    bigquery % Test,
    bigtable % Test,
    datastore % Test,
    tensorflow % Test,
    protobuf % Test,
    test % "test->test"
  )
  .enablePlugins(JmhPlugin)
