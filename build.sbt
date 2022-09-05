/*
 * Copyright 2019 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import _root_.io.github.davidgregory084._
import _root_.io.github.davidgregory084.ScalaVersion._
import scala.Ordering.Implicits._

val magnoliaScala2Version = "1.1.2"
val magnoliaScala3Version = "1.1.4"

val algebirdVersion = "0.13.9"
val avroVersion = Option(sys.props("avro.version")).getOrElse("1.11.0")
val bigqueryVersion = "v2-rev20220827-2.0.0"
val bigtableVersion = "2.11.1"
val catsVersion = "2.8.0"
val datastoreVersion = "2.11.0"
val guavaVersion = "30.1.1-jre"
val hadoopVersion = "3.3.4"
val jacksonVersion = "2.13.3"
val munitVersion = "0.7.29"
val neo4jDriverVersion = "4.4.9"
val paigesVersion = "0.4.2"
val parquetVersion = "1.12.3"
val protobufVersion = "3.21.5"
val refinedVersion = "0.10.1"
val scalacheckVersion = "1.16.0"
val shapelessVersion = "2.3.9"
val tensorflowVersion = "0.4.1"

lazy val currentYear = java.time.LocalDate.now().getYear
lazy val keepExistingHeader =
  HeaderCommentStyle.cStyleBlockComment.copy(commentCreator =
    (text: String, existingText: Option[String]) =>
      existingText
        .getOrElse(HeaderCommentStyle.cStyleBlockComment.commentCreator(text))
        .trim()
  )

ThisBuild / tpolecatDefaultOptionsMode := DevMode
ThisBuild / tpolecatDevModeOptions ~= { opts =>
  val excludes = Set(
    ScalacOptions.lintPackageObjectClasses,
    ScalacOptions.privateWarnDeadCode,
    ScalacOptions.privateWarnValueDiscard,
    ScalacOptions.warnDeadCode,
    ScalacOptions.warnValueDiscard
  )

  val parallelism = math.min(java.lang.Runtime.getRuntime.availableProcessors(), 16)
  val extras = Set(
    // required by magnolia for accessing default values
    ScalacOptions.privateOption("retain-trees", _ >= V3_0_0),
    // allow some nested auto derivation
    ScalacOptions.advancedOption("max-inlines", List("64"), _ >= V3_0_0),
    ScalacOptions.other("-target:jvm-1.8"),
    ScalacOptions.warnOption("macros:after", _.isBetween(V2_13_0, V3_0_0)),
    ScalacOptions.privateWarnOption("macros:after", _.isBetween(V2_12_0, V2_13_0)),
    ScalacOptions.privateBackendParallelism(parallelism),
    ScalacOptions.release("8")
  )

  opts.filterNot(excludes).union(extras)
}

val commonSettings = Seq(
  organization := "com.spotify",
  crossScalaVersions := Seq("2.13.8", "2.12.16"),
  scalaVersion := crossScalaVersions.value.head,
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          "com.softwaremill.magnolia1_3" %% "magnolia" % magnoliaScala3Version
        )
      case Some((2, _)) =>
        Seq(
          "com.softwaremill.magnolia1_2" %% "magnolia" % magnoliaScala2Version,
          "com.chuusai" %% "shapeless" % shapelessVersion,
          "org.scala-lang" % "scala-reflect" % scalaVersion.value
        )
      case _ =>
        throw new Exception("Unsupported scala version")
    }
  },
  // https://github.com/typelevel/scalacheck/pull/427#issuecomment-424330310
  // FIXME: workaround for Java serialization issues
  Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
  Test / publishArtifact := false,
  sonatypeProfileName := "com.spotify",
  organizationName := "Spotify AB",
  startYear := Some(2016),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  headerLicense := Some(HeaderLicense.ALv2(currentYear.toString, "Spotify AB")),
  headerMappings ++= Map(
    HeaderFileType.scala -> keepExistingHeader,
    HeaderFileType.java -> keepExistingHeader
  ),
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
    commonSettings,
    noPublishSettings,
    name := "magnolify",
    description := "A collection of Magnolia add-on modules"
  )
  .aggregate(
    avro,
    bigquery,
    bigtable,
    cats,
    datastore,
    guava,
    parquet,
    protobuf,
    refined,
    scalacheck,
    shared,
    tensorflow,
    neo4j,
    test,
    tools
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
    commonSettings,
    noPublishSettings,
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
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Test,
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Test,
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Test,
      "org.apache.avro" % "avro" % avroVersion % Test,
      "org.scalameta" %% "munit" % munitVersion % Test,
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Test
    )
  )
  .dependsOn(
    avro % Provided,
    bigquery % Provided,
    bigtable % Provided,
    datastore % Provided,
    guava % "provided,test->test",
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
      "org.apache.avro" % "avro" % avroVersion % Provided,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % Test
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
      "org.apache.hadoop" % "hadoop-client" % hadoopVersion % Provided,
      "org.apache.parquet" % "parquet-avro" % parquetVersion % Provided,
      "org.apache.parquet" % "parquet-hadoop" % parquetVersion % Provided
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
    Compile / sourceDirectories := (Compile / sourceDirectories).value
      .filterNot(_.getPath.endsWith("/src_managed/main")),
    Compile / managedSourceDirectories := (Compile / managedSourceDirectories).value
      .filterNot(_.getPath.endsWith("/src_managed/main")),
    libraryDependencies ++= Seq(
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Provided
    ),
    // Protobuf plugin adds protobuf-java to Compile scope automatically; we want it to remain Provided
    libraryDependencies := libraryDependencies.value.map { l =>
      (l.organization, l.name) match {
        case ("com.google.protobuf", "protobuf-java") =>
          l.withConfigurations(Some("provided,protobuf"))
        case _ => l
      }
    }
  )
  .dependsOn(
    shared,
    cats % Test,
    scalacheck % Test,
    test % "test->test"
  )
  .enablePlugins(ProtobufPlugin)

lazy val neo4j: Project = project
  .in(file("neo4j"))
  .settings(
    commonSettings,
    moduleName := "magnolify-neo4j",
    description := "Magnolia add-on for Neo4j",
    libraryDependencies ++= Seq(
      "org.neo4j.driver" % "neo4j-java-driver" % neo4jDriverVersion % Provided
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
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Test,
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Test,
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Test,
      "org.apache.avro" % "avro" % avroVersion % Test,
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Test
    )
  )
  .dependsOn(
    avro % Test,
    bigquery % Test,
    bigtable % Test,
    cats % Test,
    datastore % Test,
    guava % Test,
    protobuf % Test,
    scalacheck % Test,
    tensorflow % Test,
    test % "test->test"
  )
  .enablePlugins(JmhPlugin)
