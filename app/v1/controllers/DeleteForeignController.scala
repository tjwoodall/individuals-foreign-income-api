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

package v1.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.config.AppConfig
import shared.controllers._
import shared.routing.Version1
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v1.controllers.requestParsers.DeleteForeignRequestParser
import v1.models.request.delete.DeleteForeignRawData
import v1.services.DeleteForeignService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteForeignController @Inject() (val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         parser: DeleteForeignRequestParser,
                                         service: DeleteForeignService,
                                         auditService: AuditService,
                                         cc: ControllerComponents,
                                         val idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "DeleteForeignController",
      endpointName = "deleteForeign"
    )

  def deleteForeign(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData: DeleteForeignRawData = DeleteForeignRawData(nino = nino, taxYear = taxYear)

      val requestHandler = RequestHandlerOld
        .withParser(parser)
        .withService(service.deleteForeign)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "DeleteForeignIncome",
          transactionName = "delete-foreign-income",
          apiVersion = Version1,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          requestBody = None
        ))

      requestHandler.handleRequest(rawData)
    }

}
