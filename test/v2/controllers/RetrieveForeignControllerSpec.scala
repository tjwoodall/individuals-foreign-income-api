/*
 * Copyright 2024 HM Revenue & Customs
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

package v2.controllers

import config.MockForeignIncomeConfig
import play.api.Configuration
import play.api.mvc.Result
import shared.config.MockAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v1.controllers.validators.MockRetrieveForeignValidatorFactory
import v1.fixtures.RetrieveForeignFixture.fullRetrieveForeignResponseJson
import v1.models.request.retrieve
import v1.models.request.retrieve.RetrieveForeignRequest
import v1.models.response.retrieve.{ForeignEarnings, RetrieveForeignResponse, UnremittableForeignIncome}
import v1.services.MockRetrieveForeignService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveForeignControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveForeignService
    with MockRetrieveForeignValidatorFactory
    with MockAppConfig {

  private val taxYear: String = "2019-20"

  private val requestData: RetrieveForeignRequest = retrieve.RetrieveForeignRequest(
    nino = Nino(validNino),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  private val fullForeignEarningsModel: ForeignEarnings = ForeignEarnings(
    customerReference = Some("FOREIGNINCME123A"),
    earningsNotTaxableUK = 1999.99
  )

  private val fullUnremittableForeignIncomeModel1: UnremittableForeignIncome = UnremittableForeignIncome(
    countryCode = "FRA",
    amountInForeignCurrency = 1999.99,
    amountTaxPaid = Some(1999.99)
  )

  private val fullUnremittableForeignIncomeModel2: UnremittableForeignIncome = UnremittableForeignIncome(
    countryCode = "IND",
    amountInForeignCurrency = 2999.99,
    amountTaxPaid = Some(2999.99)
  )

  private val retrieveForeignResponse = RetrieveForeignResponse(
    submittedOn = Timestamp("2019-04-04T01:01:01.000Z"),
    foreignEarnings = Some(fullForeignEarningsModel),
    unremittableForeignIncome = Some(
      List(
        fullUnremittableForeignIncomeModel1,
        fullUnremittableForeignIncomeModel2
      ))
  )

  private val mtdResponse = fullRetrieveForeignResponseJson

  "RetrieveForeignController" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))
        MockedRetrieveForeignService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveForeignResponse))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponse))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))
        MockedRetrieveForeignService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with MockForeignIncomeConfig {

    val controller = new RetrieveForeignController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveForeignValidatorFactory,
      service = mockRetrieveForeignService,
      cc = cc,
      idGenerator = mockIdGenerator,
      foreignIncomeConfig = mockForeignIncomeConfig
    )

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def callController(): Future[Result] = controller.retrieveForeign(validNino, taxYear)(fakeGetRequest)
  }

}
