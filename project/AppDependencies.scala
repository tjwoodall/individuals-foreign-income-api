import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "7.23.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "org.typelevel"                %% "cats-core"                 % "2.9.0",
    "com.chuusai"                  %% "shapeless"                 % "2.4.0-M1",
    "com.neovisionaries"            % "nv-i18n"                   % "1.29",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.14.2"
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"              % "3.2.15"             % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"        % "3.2.11.0"           % scope,
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.6"             % "test, it",
    "org.scalamock"          %% "scalamock"              % "5.2.0"              % "test, it",
    "com.typesafe.play"      %% "play-test"              % PlayVersion.current  % "test, it",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapPlayVersion % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"              % scope,
    "com.github.tomakehurst"  % "wiremock-jre8"          % "2.35.0"             % "test, it",
    "io.swagger.parser.v3"    % "swagger-parser-v3"      % "2.1.12"             % "test, it"
  )

}
