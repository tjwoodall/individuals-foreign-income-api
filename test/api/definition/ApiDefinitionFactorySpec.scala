/*
 * Copyright 2026 HM Revenue & Customs
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

package api.definition

import api.config.Deprecation.NotDeprecated
import api.config.MockAppConfig
import api.definition.APIStatus.{ALPHA, BETA}
import api.mocks.MockHttpClient
import api.routing.{Version, Version2}
import api.utils.UnitSpec
import cats.implicits.catsSyntaxValidatedId

import scala.language.reflectiveCalls

class ApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the expected status" in new Test {
        setupMockConfig(Version2)
        MockedAppConfig.apiStatus(Version2) returns "BETA"

        val result: APIStatus = checkBuildApiStatus(Version2)
        result shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present but invalid" should {
      "default to alpha" in new Test {
        setupMockConfig(Version2)
        MockedAppConfig.apiStatus(Version2) returns "not-a-status"

        checkBuildApiStatus(Version2) shouldBe ALPHA
      }
    }

    "the 'deprecatedOn' parameter is missing for a deprecated version" should {
      "throw an exception" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "DEPRECATED"

        MockedAppConfig
          .deprecationFor(Version2)
          .returns("deprecatedOn date is required for a deprecated version".invalid)
          .anyNumberOfTimes()

        val exception: Exception = intercept[Exception] {
          checkBuildApiStatus(Version2)
        }

        exception.getMessage shouldBe "deprecatedOn date is required for a deprecated version"
      }
    }
  }

  "definition" when {
    "called" should {
      "return a valid Definition case class" in {
        MockedAppConfig.apiGatewayContext returns "individuals/foreign-income"
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version2).returns(true).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version2).returns(NotDeprecated.valid).anyNumberOfTimes()
        MockedAppConfig.controlledAccessEnabled returns false

        val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individuals Foreign Income (MTD)",
              description = "An API for providing individual foreign income data",
              context = "individuals/foreign-income",
              categories = List("INCOME_TAX_MTD"),
              versions = List(
                APIVersion(
                  version = Version2,
                  status = BETA,
                  access = APIAccessType.PUBLIC,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "access" when {
    "the controlled access flag is enabled" should {
      "return CONTROLLED" in {
        MockedAppConfig.apiGatewayContext returns "individuals/foreign-income"
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version2).returns(true).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version2).returns(NotDeprecated.valid).anyNumberOfTimes()
        MockedAppConfig.controlledAccessEnabled returns true

        val definition: Definition = new ApiDefinitionFactory(mockAppConfig).definition

        definition.api.versions.head.access shouldBe APIAccessType.CONTROLLED
      }
    }

    "the controlled access flag is disabled" should {
      "return PUBLIC" in {
        MockedAppConfig.apiGatewayContext returns "individuals/foreign-income"
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version2).returns(true).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version2).returns(NotDeprecated.valid).anyNumberOfTimes()
        MockedAppConfig.controlledAccessEnabled returns false

        val definition: Definition = new ApiDefinitionFactory(mockAppConfig).definition

        definition.api.versions.head.access shouldBe APIAccessType.PUBLIC
      }
    }
  }

  trait Test extends UnitSpec with MockHttpClient with MockAppConfig {
    MockedAppConfig.apiGatewayContext returns "individuals/foreign-income"

    val apiDefinitionFactory: ApiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)

    def checkBuildApiStatus(version: Version): APIStatus = apiDefinitionFactory.buildAPIStatus(version)

    protected def setupMockConfig(version: Version): Unit = {
      MockedAppConfig
        .deprecationFor(version)
        .returns(NotDeprecated.valid)
        .anyNumberOfTimes()
    }

  }

}
