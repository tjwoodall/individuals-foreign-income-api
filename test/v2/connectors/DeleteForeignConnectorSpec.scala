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

package v2.connectors

import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v2.models.request.delete
import v2.models.request.delete.DeleteForeignRequest

import scala.concurrent.Future

class DeleteForeignConnectorSpec extends ConnectorSpec {

  "DeleteForeignConnector" should {
    "return the expected response for a non-TYS request" when {
      "a valid request is made" in new IfsTest with Test {
        val taxYear = "2019-20"
        val outcome = Right(ResponseWrapper(correlationId, ()))

        willDelete(
          url = url"$baseUrl/income-tax/income/foreign/AA111111A/2019-20"
        ).returns(Future.successful(outcome))

        await(connector.deleteForeign(request)) shouldBe outcome
      }
    }

    "return the expected response for a TYS request" when {
      "a valid request is made" in new IfsTest with Test {
        val taxYear = "2023-24"
        val outcome = Right(ResponseWrapper(correlationId, ()))

        willDelete(
          url = url"$baseUrl/income-tax/income/foreign/23-24/AA111111A"
        ).returns(Future.successful(outcome))

        await(connector.deleteForeign(request)) shouldBe outcome
      }
    }
  }

  trait Test { _: ConnectorTest =>
    val taxYear: String

    val connector: DeleteForeignConnector = new DeleteForeignConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    lazy val request: DeleteForeignRequest = delete.DeleteForeignRequest(Nino("AA111111A"), TaxYear.fromMtd(taxYear))
  }

}
