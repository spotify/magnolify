addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("com.github.sbt" % "sbt-site-paradox" % "1.7.0")
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.10.6")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("com.github.sbt" % "sbt-paradox-material-theme" % "0.7.0")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.5.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.11")
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.6.7")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")

// sbt-site plugin conflict. Remove after update
dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.2.0"
)
