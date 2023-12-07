import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = List(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )

  val settings: Seq[Setting[_]] = List(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true
  )

}
