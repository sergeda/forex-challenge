import Dependencies._

name := "forex"
version := "1.0.1"

scalaVersion := "2.12.10"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Ydelambdafy:method",
  "-Xlog-reflective-calls",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  compilerPlugin(Libraries.kindProjector),
  Libraries.cats,
  Libraries.catsEffect,
  Libraries.catsEffectLaws,
  Libraries.fs2,
  Libraries.http4sDsl,
  Libraries.http4sServer,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.sttpCore,
  Libraries.sttpCats,
  Libraries.circeGenericExt,
  Libraries.circeParser,
  Libraries.pureConfig,
  Libraries.logback,
  Libraries.cache2k,
  Libraries.scalaCache,
  Libraries.scalaTest           % Test,
  Libraries.scalaCheck          % Test,
  Libraries.scalaMock          % Test,
  Libraries.catsScalaCheck      % Test,
)

parallelExecution in Test := false
