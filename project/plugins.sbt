addSbtPlugin("com.github.sbt" % "sbt-protobuf" % "0.7.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.2")

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.11.4"
)
