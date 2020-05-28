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

val magnoliaVersion = "0.16.0"

val avroVersion = Option(sys.props("avro.version")).getOrElse("1.9.2")
val bigqueryVersion = "v2-rev20200429-1.30.9"
val bigtableVersion = "1.13.0"
val catsVersion = "2.1.1"
val datastoreVersion = "1.6.3"
val guavaVersion = "29.0-jre"
val jacksonVersion = "2.11.0"
val jodaTimeVersion = "2.10.6"
val protobufVersion = "3.12.1"

val scalacheckVersion = "1.14.3"
val tensorflowVersion = "1.15.0"

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.13.2",
  crossScalaVersions := Seq("2.12.11", "2.13.2"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  scalacOptions ++= (scalaBinaryVersion.value match {
    case "2.11" => Seq("-language:higherKinds")
    case "2.12" => Seq("-language:higherKinds")
    case "2.13" => Nil
  }),
  libraryDependencies ++= {
    if (scalaBinaryVersion.value == "2.11") {
      Seq("me.lyh" %% "magnolia" % "0.10.1-jto")
    } else {
      Seq(
        "com.propensive" %% "magnolia" % magnoliaVersion,
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
    }
  },
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
    // FIXME: implement these
    // diffy,
    guava,
    avro,
    bigquery,
    bigtable,
    datastore,
    protobuf,
    tensorflow,
    test
  )

lazy val shared: Project = project
  .in(file("shared"))
  .settings(
    commonSettings,
    moduleName := "magnolify-shared",
    description := "Shared code for Magnolify",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test
    )
  )

// shared code for unit tests
lazy val test: Project = project
  .in(file("test"))
  .settings(
    commonSettings ++ noPublishSettings,
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
      "org.typelevel" %% "cats-core" % catsVersion % Test
    ),
    protobufRunProtoc in ProtobufConfig := (args =>
      com.github.os72.protocjar.Protoc.runProtoc(args.toArray)
    )
  )
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
      "org.typelevel" %% "cats-laws" % catsVersion % Test
    )
  )
  .dependsOn(
    shared,
    scalacheck % Test,
    test % "test->test"
  )

lazy val diffy: Project = project
  .in(file("diffy"))
  .settings(
    commonSettings,
    moduleName := "magnolify-diffy",
    description := "Magnolia add-on for diffing data"
  )
  .dependsOn(
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
      "joda-time" % "joda-time" % jodaTimeVersion % Provided,
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
      "org.tensorflow" % "proto" % tensorflowVersion % Provided
    )
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )

lazy val jmh: Project = project
  .in(file("jmh"))
  .settings(
    commonSettings,
    sourceDirectory in Jmh := (sourceDirectory in Test).value,
    classDirectory in Jmh := (classDirectory in Test).value,
    dependencyClasspath in Jmh := (dependencyClasspath in Test).value,
    // rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile'
    // (otherwise a clean 'jmh:run' would fail)
    compile in Jmh := (compile in Jmh).dependsOn(compile in Test).value,
    run in Jmh := (run in Jmh).dependsOn(compile in Jmh).evaluated,
    libraryDependencies ++= Seq(
      "org.apache.avro" % "avro" % avroVersion % Test,
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Test,
      "com.google.cloud" % "google-cloud-bigtable" % bigtableVersion % Test,
      "joda-time" % "joda-time" % jodaTimeVersion % Test,
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Test,
      "org.tensorflow" % "proto" % tensorflowVersion % Test
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
