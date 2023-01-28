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
import sbtprotoc.ProtocPlugin.ProtobufConfig

val magnoliaScala2Version = "1.1.3"
val magnoliaScala3Version = "1.1.4"

val algebirdVersion = "0.13.9"
val avroVersion = Option(sys.props("avro.version")).getOrElse("1.11.0")
val bigqueryVersion = "v2-rev20230114-2.0.0"
val bigtableVersion = "2.18.1"
val catsVersion = "2.9.0"
val datastoreVersion = "2.13.3"
val guavaVersion = "31.1-jre"
val hadoopVersion = "3.3.4"
val jacksonVersion = "2.14.1"
val munitVersion = "0.7.29"
val neo4jDriverVersion = "4.4.9"
val paigesVersion = "0.4.2"
val parquetVersion = "1.12.3"
val protobufVersion = "3.21.12"
val refinedVersion = "0.10.1"
val scalaCollectionCompatVersion = "2.9.0"
val scalacheckVersion = "1.17.0"
val shapelessVersion = "2.3.10"
val tensorflowMetadataVersion = "1.10.0"
val tensorflowVersion = "0.4.2"

// project
ThisBuild / tlBaseVersion := "0.6"
ThisBuild / organization := "com.spotify"
ThisBuild / organizationName := "Spotify AB"
ThisBuild / startYear := Some(2016)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
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
  ),
  Developer(
    id = "shnapz",
    name = "Andrew Kabas",
    email = "akabas@spotify.com",
    url = url("https://github.com/shnapz")
  )
)

// scala versions
val scala3 = "3.2.1"
val scala213 = "2.13.10"
val scala212 = "2.12.17"
val defaultScala = scala213

// github actions
val java11 = JavaSpec.corretto("11")
val java8 = JavaSpec.corretto("8")
val defaultJava = java11
val coverageCond = Seq(
  s"matrix.scala == '$defaultScala'",
  s"matrix.java == '${defaultJava.render}'"
).mkString(" && ")

ThisBuild / scalaVersion := defaultScala
ThisBuild / crossScalaVersions := Seq(scala213, scala212)
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowJavaVersions := Seq(java11, java8)
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("coverage", "test", "coverageAggregate"),
    name = Some("Build project"),
    cond = Some(coverageCond)
  ),
  WorkflowStep.Run(
    List("bash <(curl -s https://codecov.io/bash)"),
    name = Some("Upload coverage report"),
    cond = Some(coverageCond)
  ),
  WorkflowStep.Sbt(List("test"), name = Some("Build project"), cond = Some(s"!($coverageCond)"))
)
ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "avro-legacy",
    "Test with legacy avro",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Sbt(
        List("avro/test"),
        env = Map("JAVA_OPTS" -> "-Davro.version=1.8.2"),
        name = Some("Build project")
      )
    ),
    scalas = List(defaultScala),
    javas = List(defaultJava)
  )
)

// protobuf
ThisBuild / PB.protocVersion := protobufVersion
lazy val scopedProtobufSettings = Def.settings(
  PB.targets := Seq(
    PB.gens.java -> (ThisScope.copy(config = Zero) / sourceManaged).value /
      "compiled_proto" /
      configuration.value.name
  ),
  managedSourceDirectories ++= PB.targets.value.map(_.outputPath)
)
lazy val protobufSettings = Seq(
  PB.additionalDependencies := Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion % Provided
  )
) ++ Seq(Compile, Test).flatMap(c => inConfig(c)(scopedProtobufSettings))

lazy val currentYear = java.time.LocalDate.now().getYear
lazy val keepExistingHeader =
  HeaderCommentStyle.cStyleBlockComment.copy(commentCreator =
    (text: String, existingText: Option[String]) =>
      existingText
        .getOrElse(HeaderCommentStyle.cStyleBlockComment.commentCreator(text))
        .trim()
  )

val commonSettings = Seq(
  tlFatalWarningsInCi := false,
  tlJdkRelease := Some(8),
  tlSkipIrrelevantScalas := true,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          // required by magnolia for accessing default values
          "-Xretain-trees",
          // tolerate some nested macro expansion
          "-Ymax-inlines",
          "64"
        )
      case Some((2, 13)) =>
        Seq(
          // silence warnings
          "-Wmacros:after",
          "-Wconf:cat=unused-imports&origin=scala\\.collection\\.compat\\..*:s" +
            ",cat=unused-imports&origin=magnolify\\.shims\\..*:s"
        )
      case Some((2, 12)) =>
        Seq(
          "-Ywarn-macros:after"
        )
      case _ =>
        Seq.empty
    }
  },
  headerLicense := Some(HeaderLicense.ALv2(currentYear.toString, organizationName.value)),
  headerMappings ++= Map(
    HeaderFileType.scala -> keepExistingHeader,
    HeaderFileType.java -> keepExistingHeader
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          "com.softwaremill.magnolia1_3" %% "magnolia" % magnoliaScala3Version,
          "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion
        )
      case Some((2, _)) =>
        Seq(
          "com.softwaremill.magnolia1_2" %% "magnolia" % magnoliaScala2Version,
          "com.chuusai" %% "shapeless" % shapelessVersion,
          "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion,
          "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
        )
      case _ =>
        throw new Exception("Unsupported scala version")
    }
  },
  // https://github.com/typelevel/scalacheck/pull/427#issuecomment-424330310
  // FIXME: workaround for Java serialization issues
  Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
)

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .settings(
    commonSettings,
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

lazy val shared = project
  .in(file("shared"))
  .settings(
    commonSettings,
    moduleName := "magnolify-shared",
    description := "Shared code for Magnolify"
  )

// shared code for unit tests
lazy val test = project
  .in(file("test"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(shared)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %% "cats-core" % catsVersion % Test
    )
  )

lazy val scalacheck = project
  .in(file("scalacheck"))
  .dependsOn(
    shared,
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-scalacheck",
    description := "Magnolia add-on for ScalaCheck",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % scalacheckVersion % Provided
  )

lazy val cats = project
  .in(file("cats"))
  .dependsOn(
    shared,
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-cats",
    description := "Magnolia add-on for Cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion % Provided,
      "com.twitter" %% "algebird-core" % algebirdVersion % Test,
      "org.typelevel" %% "cats-laws" % catsVersion % Test
    )
  )

lazy val guava = project
  .in(file("guava"))
  .dependsOn(
    shared,
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-guava",
    description := "Magnolia add-on for Guava",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % guavaVersion % Provided
    )
  )

lazy val refined = project
  .in(file("refined"))
  .dependsOn(
    avro % Provided,
    bigquery % Provided,
    bigtable % Provided,
    datastore % Provided,
    guava % "provided,test->test",
    protobuf % "provided,test->test",
    tensorflow % Provided,
    test % "test->test"
  )
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

lazy val avro = project
  .in(file("avro"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-avro",
    description := "Magnolia add-on for Apache Avro",
    libraryDependencies ++= Seq(
      "org.apache.avro" % "avro" % avroVersion % Provided,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % Test
    )
  )

lazy val bigquery = project
  .in(file("bigquery"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-bigquery",
    description := "Magnolia add-on for Google Cloud BigQuery",
    libraryDependencies ++= Seq(
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion % Provided,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % Test
    )
  )

lazy val bigtable: Project = project
  .in(file("bigtable"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-bigtable",
    description := "Magnolia add-on for Google Cloud Bigtable",
    libraryDependencies ++= Seq(
      "com.google.api.grpc" % "proto-google-cloud-bigtable-v2" % bigtableVersion % Provided
    )
  )

lazy val datastore = project
  .in(file("datastore"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-datastore",
    description := "Magnolia add-on for Google Cloud Datastore",
    libraryDependencies ++= Seq(
      "com.google.cloud.datastore" % "datastore-v1-proto-client" % datastoreVersion % Provided
    )
  )

lazy val parquet = project
  .in(file("parquet"))
  .dependsOn(
    shared,
    avro % Test,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-parquet",
    description := "Magnolia add-on for Apache Parquet",
    libraryDependencies ++= Seq(
      "org.apache.hadoop" % "hadoop-client" % hadoopVersion % Provided,
      "org.apache.parquet" % "parquet-avro" % parquetVersion % Provided,
      "org.apache.parquet" % "parquet-hadoop" % parquetVersion % Provided
    ),
    dependencyOverrides ++= Seq(
      "org.apache.avro" % "avro" % avroVersion % Provided,
      "org.apache.avro" % "avro" % avroVersion % Test
    )
  )

lazy val protobuf = project
  .in(file("protobuf"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    protobufSettings,
    moduleName := "magnolify-protobuf",
    description := "Magnolia add-on for Google Protocol Buffer"
  )

val unpackMetadata = taskKey[Seq[File]]("Unpack tensorflow metadata proto files.")

lazy val tensorflow = project
  .in(file("tensorflow"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    protobufSettings,
    moduleName := "magnolify-tensorflow",
    description := "Magnolia add-on for TensorFlow",
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java" % protobufVersion % ProtobufConfig,
      "org.tensorflow" % "tensorflow-core-api" % tensorflowVersion % Provided
    ),
    // tensorflow metadata protos are not packaged into a jar. Manually extract them as external
    unpackMetadata := {
      IO.unzipURL(
        new URL(
          s"https://github.com/tensorflow/metadata/archive/refs/tags/v$tensorflowMetadataVersion.zip"
        ),
        target.value,
        "**/*.proto"
      )
      IO.move(target.value / s"metadata-$tensorflowMetadataVersion", PB.externalSourcePath.value)
      IO.listFiles(PB.externalSourcePath.value / s"metadata-$tensorflowMetadataVersion")
    },
    Compile / PB.generate := (Compile / PB.generate).dependsOn(unpackMetadata).value,
    Compile / packageBin / mappings ~= {
      _.filterNot { case (_, n) => n.startsWith("org/tensorflow") }
    },
    // Something messes with Compile/packageSrc/mappings and adds protobuf managed sources
    // Force back to original value from sbt
    inConfig(Compile)(Defaults.packageTaskSettings(packageSrc, Defaults.packageSrcMappings))
  )

lazy val neo4j = project
  .in(file("neo4j"))
  .dependsOn(
    shared,
    cats % "test->test",
    scalacheck % "test->test",
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-neo4j",
    description := "Magnolia add-on for Neo4j",
    libraryDependencies ++= Seq(
      "org.neo4j.driver" % "neo4j-java-driver" % neo4jDriverVersion % Provided
    )
  )

lazy val tools = project
  .in(file("tools"))
  .dependsOn(
    shared,
    avro % Test,
    bigquery % Test,
    parquet % Test,
    test % "test->test"
  )
  .settings(
    commonSettings,
    moduleName := "magnolify-tools",
    description := "Magnolia add-on for code generation",
    libraryDependencies ++= Seq(
      "com.google.apis" % "google-api-services-bigquery" % bigqueryVersion,
      "org.apache.avro" % "avro" % avroVersion % Provided,
      "org.apache.parquet" % "parquet-hadoop" % parquetVersion,
      "org.typelevel" %% "paiges-core" % paigesVersion
    )
  )

lazy val jmh: Project = project
  .in(file("jmh"))
  .enablePlugins(JmhPlugin)
  .dependsOn(
    avro % Test,
    bigquery % Test,
    bigtable % Test,
    cats % Test,
    datastore % Test,
    guava % Test,
    protobuf % "test->test",
    scalacheck % Test,
    tensorflow % Test,
    test % "test->test"
  )
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
