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

package v1.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.mocks.MockAppConfig
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.{ErrorWrapper, NinoFormatError, RuleTaxYearNotSupportedError}
import shared.models.outcomes.ResponseWrapper
import shared.services.MockAuditService
import v1.controllers.requestParsers.MockCreateAmendForeignRequestParser
import v1.models.request.createAmend
import v1.models.request.createAmend._
import v1.services.MockCreateAmendForeignService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendForeignControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAuditService
    with MockCreateAmendForeignService
    with MockCreateAmendForeignRequestParser
    with MockAppConfig {

  val taxYear: String = "2019-20"

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |      "customerReference": "FOREIGNINCME123A",
      |      "earningsNotTaxableUK": 1999.99
      |   },
      |   "unremittableForeignIncome": [
      |       {
      |          "countryCode": "FRA",
      |          "amountInForeignCurrency": 1999.99,
      |          "amountTaxPaid": 1999.99
      |       },
      |       {
      |          "countryCode": "IND",
      |          "amountInForeignCurrency": 2999.99,
      |          "amountTaxPaid": 2999.99
      |       }
      |    ]
      |}
    """.stripMargin
  )

  val rawData: CreateAmendForeignRawData = createAmend.CreateAmendForeignRawData(
    nino = nino,
    taxYear = taxYear,
    body = AnyContentAsJson(requestBodyJson)
  )

  val foreignEarning: ForeignEarnings = ForeignEarnings(
    customerReference = Some("FOREIGNINCME123A"),
    earningsNotTaxableUK = 1999.99
  )

  val unremittableForeignIncomeItems: Seq[UnremittableForeignIncomeItem] = List(
    UnremittableForeignIncomeItem(
      countryCode = "FRA",
      amountInForeignCurrency = 1999.99,
      amountTaxPaid = Some(1999.99)
    ),
    UnremittableForeignIncomeItem(
      countryCode = "IND",
      amountInForeignCurrency = 2999.99,
      amountTaxPaid = Some(2999.99)
    )
  )

  val amendForeignRequestBody: CreateAmendForeignRequestBody = createAmend.CreateAmendForeignRequestBody(
    foreignEarnings = Some(foreignEarning),
    unremittableForeignIncome = Some(unremittableForeignIncomeItems)
  )

  val requestData: CreateAmendForeignRequest =
    createAmend.CreateAmendForeignRequest(nino = Nino(nino), taxYear = TaxYear.fromMtd(taxYear), body = amendForeignRequestBody)

  "AmendForeignController" should {
    "return a successful response with status 200 (OK)" when {
      "the request received is valid" in new Test {
          MockedCreateAmendForeignRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockedCreateAmendForeignService
          .amendForeign(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeAuditRequestBody = Some(requestBodyJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
          MockedCreateAmendForeignRequestParser
          .parse(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "the service returns an error" in new Test {
          MockedCreateAmendForeignRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockedCreateAmendForeignService
          .amendForeign(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, Some(requestBodyJson))
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller = new CreateAmendForeignController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockCreateAmendForeignRequestParser,
      service = mockCreateAmendForeignService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.createAmendForeign(nino, taxYear)(fakePutRequest(requestBodyJson))

    def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendForeignIncome",
        transactionName = "create-amend-foreign-income",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

}
