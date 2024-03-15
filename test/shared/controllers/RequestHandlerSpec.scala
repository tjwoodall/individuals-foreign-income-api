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

package shared.controllers

import org.scalamock.handlers.CallHandler
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsString, Json, OWrites}
import play.api.mvc.AnyContent
import play.api.test.{FakeRequest, ResultExtractors}
import shared.controllers.requestParsers.RequestParser
import shared.mocks.MockAppConfig
import shared.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.auth.UserDetails
import shared.models.errors.{ErrorWrapper, NinoFormatError}
import shared.models.outcomes.ResponseWrapper
import shared.models.request.RawData
import shared.services.{MockAuditService, ServiceOutcome}
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class RequestHandlerSpec
    extends UnitSpec
    with MockAuditService
    with MockIdGenerator
    with Status
    with HeaderNames
    with ResultExtractors
    with MockAppConfig {

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private val successResponseJson = Json.obj("result" -> "SUCCESS!")
  private val successCode         = Status.ACCEPTED

  private val generatedCorrelationId = "generatedCorrelationId"
  private val serviceCorrelationId   = "serviceCorrelationId"

  case object InputRaw extends RawData
  case object Input
  case object Output { implicit val writes: OWrites[Output.type] = _ => successResponseJson }

  MockIdGenerator.generateCorrelationId.returns(generatedCorrelationId).anyNumberOfTimes()

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SomeController", endpointName = "someEndpoint")

  implicit val hc: HeaderCarrier                    = HeaderCarrier()
  implicit val ctx: RequestContext                  = RequestContext.from(mockIdGenerator, endpointLogContext)
  private val userDetails                           = UserDetails("mtdId", "Individual", Some("agentReferenceNumber"))
  implicit val userRequest: UserRequest[AnyContent] = UserRequest[AnyContent](userDetails, FakeRequest())

  trait DummyService {
    def service(input: Input.type)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Output.type]]
  }

  private val mockService = mock[DummyService]

  private def service =
    (mockService.service(_: Input.type)(_: RequestContext, _: ExecutionContext)).expects(Input, *, *).anyNumberOfTimes()

  private val mockParser = mock[RequestParser[InputRaw.type, Input.type]]

  private def parseRequest =
    (mockParser.parseRequest(_: InputRaw.type)(_: String)).expects(InputRaw, *).anyNumberOfTimes()

  "RequestHandler" when {
    "given a request" must {
      "return the correct response" in {
        val requestHandler = RequestHandler
          .withParser(mockParser)
          .withService(mockService.service)
          .withPlainJsonResult(successCode)

        parseRequest returns Right(Input)
        service returns Future.successful(Right(ResponseWrapper(serviceCorrelationId, Output)))

        val result = requestHandler.handleRequest(InputRaw)

        contentAsJson(result) shouldBe successResponseJson
        header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
        status(result) shouldBe successCode
      }

      "return no content if required" in {
        val requestHandler = RequestHandler
          .withParser(mockParser)
          .withService(mockService.service)
          .withNoContentResult()

        parseRequest returns Right(Input)
        service returns Future.successful(Right(ResponseWrapper(serviceCorrelationId, Output)))

        val result = requestHandler.handleRequest(InputRaw)

        contentAsString(result) shouldBe ""
        header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
        status(result) shouldBe NO_CONTENT
      }
    }

    "given a request with a RequestCannotBeFulfilled gov-test-scenario header" when {
      "allowed in config" should {
        "return RuleRequestCannotBeFulfilled error" in {
          val requestHandler = RequestHandler
            .withParser(mockParser)
            .withService(mockService.service)
            .withNoContentResult()

          MockedAppConfig.allowRequestCannotBeFulfilledHeader.returns(true).anyNumberOfTimes()

          val ctx2: RequestContext = ctx.copy(hc = hc.copy(otherHeaders = List("Gov-Test-Scenario" -> "REQUEST_CANNOT_BE_FULFILLED")))

          val result = requestHandler.handleRequest(InputRaw)(ctx2, userRequest, mockAppConfig, ec)

          status(result) shouldBe 422
          header("X-CorrelationId", result) shouldBe Some(generatedCorrelationId)
          contentAsJson(result) shouldBe Json.parse(
            """
              |{
              |  "code":"RULE_REQUEST_CANNOT_BE_FULFILLED",
              |  "message":"Custom (will vary in production depending on the actual error)"
              |}
              |""".stripMargin
          )
        }
      }

      "not allowed in config" should {
        "return success response" in {
          val requestHandler = RequestHandler
            .withParser(mockParser)
            .withService(mockService.service)
            .withPlainJsonResult(successCode)

          parseRequest returns Right(Input)
          service returns Future.successful(Right(ResponseWrapper(serviceCorrelationId, Output)))
          MockedAppConfig.allowRequestCannotBeFulfilledHeader.returns(false).anyNumberOfTimes()

          val ctx2: RequestContext = ctx.copy(hc = hc.copy(otherHeaders = List("Gov-Test-Scenario" -> "REQUEST_CANNOT_BE_FULFILLED")))

          val result = requestHandler.handleRequest(InputRaw)(ctx2, userRequest, mockAppConfig, ec)

          contentAsJson(result) shouldBe successResponseJson
          header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
          status(result) shouldBe successCode
        }
      }

    }

    "a request fails with validation errors" must {
      "return the errors" in {
        val requestHandler = RequestHandler
          .withParser(mockParser)
          .withService(mockService.service)
          .withPlainJsonResult(successCode)

        parseRequest returns Left(ErrorWrapper(generatedCorrelationId, NinoFormatError))

        val result = requestHandler.handleRequest(InputRaw)

        contentAsJson(result) shouldBe NinoFormatError.asJson
        header("X-CorrelationId", result) shouldBe Some(generatedCorrelationId)
        status(result) shouldBe NinoFormatError.httpStatus
      }
    }

    "a request fails with service errors" must {
      "return the errors" in {
        val requestHandler = RequestHandler
          .withParser(mockParser)
          .withService(mockService.service)
          .withPlainJsonResult(successCode)

        parseRequest returns Right(Input)
        service returns Future.successful(Left(ErrorWrapper(serviceCorrelationId, NinoFormatError)))

        val result = requestHandler.handleRequest(InputRaw)

        contentAsJson(result) shouldBe NinoFormatError.asJson
        header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
        status(result) shouldBe NinoFormatError.httpStatus
      }
    }

    "auditing is configured" when {
      val params = Map("param" -> "value")

      val auditType = "type"
      val txName    = "txName"

      val requestBody = Some(JsString("REQUEST BODY"))

      def auditHandler(includeResponse: Boolean = false): AuditHandler = AuditHandler(
        mockAuditService,
        auditType = auditType,
        transactionName = txName,
        params = params,
        requestBody = requestBody,
        includeResponse = includeResponse
      )

      val basicRequestHandler = RequestHandler
        .withParser(mockParser)
        .withService(mockService.service)
        .withPlainJsonResult(successCode)

      def verifyAudit(correlationId: String, auditResponse: AuditResponse): CallHandler[Future[AuditResult]] =
        MockedAuditService.verifyAuditEvent(
          AuditEvent(
            auditType = auditType,
            transactionName = txName,
            GenericAuditDetail(userDetails, params = params, request = requestBody, `X-CorrelationId` = correlationId, auditResponse)
          ))

      "a request is successful" when {
        "no response is to be audited" must {
          "audit without the response" in {
            val requestHandler = basicRequestHandler.withAuditing(auditHandler())

            parseRequest returns Right(Input)
            service returns Future.successful(Right(ResponseWrapper(serviceCorrelationId, Output)))

            val result = requestHandler.handleRequest(InputRaw)

            contentAsJson(result) shouldBe successResponseJson
            header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
            status(result) shouldBe successCode

            verifyAudit(serviceCorrelationId, AuditResponse(successCode, Right(None)))
          }
        }

        "the response is to be audited" must {
          "audit with the response" in {
            val requestHandler = basicRequestHandler.withAuditing(auditHandler(includeResponse = true))

            parseRequest returns Right(Input)
            service returns Future.successful(Right(ResponseWrapper(serviceCorrelationId, Output)))

            val result = requestHandler.handleRequest(InputRaw)

            contentAsJson(result) shouldBe successResponseJson
            header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
            status(result) shouldBe successCode

            verifyAudit(serviceCorrelationId, AuditResponse(successCode, Right(Some(successResponseJson))))
          }
        }
      }

      "a request fails with validation errors" must {
        "audit the failure" in {
          val requestHandler = basicRequestHandler.withAuditing(auditHandler())

          parseRequest returns Left(ErrorWrapper(generatedCorrelationId, NinoFormatError))

          val result = requestHandler.handleRequest(InputRaw)

          contentAsJson(result) shouldBe NinoFormatError.asJson
          header("X-CorrelationId", result) shouldBe Some(generatedCorrelationId)
          status(result) shouldBe NinoFormatError.httpStatus

          verifyAudit(generatedCorrelationId, AuditResponse(NinoFormatError.httpStatus, Left(List(AuditError(NinoFormatError.code)))))
        }
      }

      "a request fails with service errors" must {
        "audit the failure" in {
          val requestHandler = basicRequestHandler.withAuditing(auditHandler())

          parseRequest returns Right(Input)
          service returns Future.successful(Left(ErrorWrapper(serviceCorrelationId, NinoFormatError)))

          val result = requestHandler.handleRequest(InputRaw)

          contentAsJson(result) shouldBe NinoFormatError.asJson
          header("X-CorrelationId", result) shouldBe Some(serviceCorrelationId)
          status(result) shouldBe NinoFormatError.httpStatus

          verifyAudit(serviceCorrelationId, AuditResponse(NinoFormatError.httpStatus, Left(List(AuditError(NinoFormatError.code)))))
        }
      }
    }
  }

}
