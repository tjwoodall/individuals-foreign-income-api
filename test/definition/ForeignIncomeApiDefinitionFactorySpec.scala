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

package definition

import api.config.Deprecation.NotDeprecated
import api.config.MockAppConfig
import api.definition.APIStatus.BETA
import api.definition.{APIDefinition, APIVersion, Definition}
import api.routing.Version2
import api.utils.UnitSpec
import cats.implicits.catsSyntaxValidatedId

class ForeignIncomeApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  "definition" when {
    "called" should {
      "return a valid Definition case class" in {
        MockedAppConfig.apiGatewayContext returns "individuals/foreign-income"

        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version2).returns(true).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version2).returns(NotDeprecated.valid).anyNumberOfTimes()

        val apiDefinitionFactory = new ForeignIncomeApiDefinitionFactory(mockAppConfig)

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individuals Foreign Income (MTD)",
              description = "An API for providing individual foreign income data",
              context = "individuals/foreign-income",
              categories = List("INCOME_TAX_MTD"),
              versions = List(
                APIVersion(
                  Version2,
                  status = BETA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

}
