/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package auth

import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import shared.auth.AuthSupportingAgentsAllowedISpec
import shared.services.DownstreamStub

class IndividualsForeignIncomeAuthSupportingAgentsAllowedISpec extends AuthSupportingAgentsAllowedISpec {

  val callingApiVersion = "1.0"

  val supportingAgentsAllowedEndpoint = "create-amend-foreign"

  val mtdUrl = s"/$nino/2019-20"

  def sendMtdRequest(request: WSRequest): WSResponse = await(
    request.put(
      Json.parse("""
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
         |""".stripMargin
      )
    )
  )

  val downstreamUri: String = s"/income-tax/income/foreign/$nino/2019-20"

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  override val downstreamSuccessStatus: Int = NO_CONTENT

  override val expectedMtdSuccessStatus: Int = OK

  val maybeDownstreamResponseJson: Option[JsValue] = None

}
