/*
 * Copyright 2025 HM Revenue & Customs
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

package v1.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import shared.connectors.DownstreamOutcome
import uk.gov.hmrc.http.HeaderCarrier
import v1.models.request.createAmend.CreateAmendForeignRequest

import scala.concurrent.{ExecutionContext, Future}

trait MockCreateAmendForeignConnector extends TestSuite with MockFactory {

  val mockCreateAmendForeignConnector: CreateAmendForeignConnector = mock[CreateAmendForeignConnector]

  object MockedCreateAmendForeignConnector {

    def amendForeign(request: CreateAmendForeignRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (mockCreateAmendForeignConnector
        .amendForeign(_: CreateAmendForeignRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(request, *, *, *)
    }

  }

}
