/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

trait AppConfig {

  // DES Config
  def desBaseUrl: String
  def desEnv: String
  def desToken: String
  def desEnvironmentHeaders: Option[Seq[String]]

  def desDownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = desBaseUrl, env = desEnv, token = desToken, environmentHeaders = desEnvironmentHeaders)

  // IFS Config
  def ifsBaseUrl: String
  def ifsEnv: String
  def ifsToken: String
  def ifsEnvironmentHeaders: Option[Seq[String]]

  def ifsDownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = ifsBaseUrl, env = ifsEnv, token = ifsToken, environmentHeaders = ifsEnvironmentHeaders)

  // Tax Year Specific (TYS) IFS Config
  def tysIfsBaseUrl: String
  def tysIfsEnv: String
  def tysIfsToken: String
  def tysIfsEnvironmentHeaders: Option[Seq[String]]

  def tysIfsDownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = tysIfsBaseUrl, env = tysIfsEnv, token = tysIfsToken, environmentHeaders = tysIfsEnvironmentHeaders)

  // MTD IF Lookup Config
  def mtdIdBaseUrl: String
  def featureSwitches: Configuration
  def apiStatus(version: String): String
  def apiGatewayContext: String
  def endpointsEnabled(version: String): Boolean

  def minimumPermittedTaxYear: Int

  def confidenceLevelConfig: ConfidenceLevelConfig
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig, configuration: Configuration) extends AppConfig {

  val mtdIdBaseUrl: String = config.baseUrl("mtd-id-lookup")

  // DES Config
  val desBaseUrl: String                         = config.baseUrl("des")
  val desEnv: String                             = config.getString("microservice.services.des.env")
  val desToken: String                           = config.getString("microservice.services.des.token")
  val desEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.des.environmentHeaders")

  // IFS Config
  val ifsBaseUrl: String                         = config.baseUrl("ifs")
  val ifsEnv: String                             = config.getString("microservice.services.ifs.env")
  val ifsToken: String                           = config.getString("microservice.services.ifs.token")
  val ifsEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifs.environmentHeaders")

  // Tax Year Specific (TYS) IFS Config
  val tysIfsBaseUrl: String                         = config.baseUrl("tys-ifs")
  val tysIfsEnv: String                             = config.getString("microservice.services.tys-ifs.env")
  val tysIfsToken: String                           = config.getString("microservice.services.tys-ifs.token")
  val tysIfsEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.tys-ifs.environmentHeaders")

  // MTD IF Lookup Config
  val apiGatewayContext: String                    = config.getString("api.gateway.context")
  def apiStatus(version: String): String           = config.getString(s"api.$version.status")
  def featureSwitches: Configuration               = configuration.getOptional[Configuration](s"feature-switch").getOrElse(Configuration.empty)
  def endpointsEnabled(version: String): Boolean   = config.getBoolean(s"api.$version.endpoints.enabled")
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")

  val minimumPermittedTaxYear: Int = config.getInt("minimumPermittedTaxYear")
}

case class ConfidenceLevelConfig(confidenceLevel: ConfidenceLevel, definitionEnabled: Boolean, authValidationEnabled: Boolean)

object ConfidenceLevelConfig {

  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      confidenceLevel = ConfidenceLevel.fromInt(config.getInt("confidence-level")).getOrElse(ConfidenceLevel.L200),
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }

}
