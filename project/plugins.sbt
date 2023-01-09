addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.15")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.9.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.2")

// force usage of scala-xml v2
// See https://github.com/scoverage/sbt-scoverage/issues/439
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
