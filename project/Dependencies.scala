package quasar.s3.project

import scala.Boolean
import scala.collection.Seq

import sbt._

object Dependencies {
  private val http4sVersion = "0.16.6a"

  // hooray, scala-xml. we use it for parsing XML out of
  // the S3 API's responses.
  private val scalaXmlVersion = "1.1.0"

  // used for parsing JSON out of the stream of S3 data
  // in an object.
  // we need to be compatible with Quasar's version of both
  // fs2 and jawn, so we use the older circe-jawn version.
  private val circeJawnVersion = "0.10.0-M1"

  private val quasarVersion = IO.read(file("./quasar-version")).trim

  private val specsVersion = "4.2.0"

  // http4s-blaze-client's version has to be in sync with
  // quasar's http4s version. The same goes for any
  // dependencies, transitive or otherwise.
  def lwcCore = Seq(
    "org.http4s"             %% "http4s-scala-xml"    % http4sVersion,
    "org.http4s"             %% "http4s-blaze-client" % http4sVersion,
    "org.scala-lang.modules" %% "scala-xml"           % scalaXmlVersion,
    "io.circe"               %% "circe-jawn"          % circeJawnVersion
  )

  // we need to separate quasar out from the LWC dependencies,
  // to keep from packaging it and its dependencies.
  // TODO: we should do this in the assembly routine.
  def lwc = lwcCore ++ Seq(
    "com.slamdata" %% "quasar-mimir-internal" % quasarVersion
  )

  // no extra dependencies for integration tests, for now.
  def it = lwc ++ Seq(
    "org.specs2" %% "specs2-core"       % specsVersion % Test,
    "org.specs2" %% "specs2-scalacheck" % specsVersion % Test,
    "org.specs2" %% "specs2-scalaz"     % specsVersion % Test
  )
}
