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

package v2.controllers.validators

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import config.ForeignIncomeConfig
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v1.controllers.validators.CreateAmendForeignValidator.validateBusinessRules
import v1.models.request.createAmend.{CreateAmendForeignRequest, CreateAmendForeignRequestBody}

import javax.inject.{Inject, Singleton}

@Singleton
class CreateAmendForeignValidatorFactory @Inject() (implicit foreignIncomeConfig: ForeignIncomeConfig) {
  private lazy val minTaxYear = foreignIncomeConfig.minimumPermittedTaxYear()
  private val resolveJson     = new ResolveNonEmptyJsonObject[CreateAmendForeignRequestBody]()

  def validator(nino: String, taxYear: String, body: JsValue): Validator[CreateAmendForeignRequest] = new Validator[CreateAmendForeignRequest] {

    def validate: Validated[Seq[MtdError], CreateAmendForeignRequest] =
      (
        ResolveNino(nino),
        ResolveTaxYearMinimum(TaxYear.fromDownstreamInt(minTaxYear)).apply(taxYear),
        resolveJson(body)
      ).mapN((resolvedNino, resolvedTaxYear, resolvedBody) =>
        CreateAmendForeignRequest(resolvedNino, resolvedTaxYear, resolvedBody)) andThen validateBusinessRules

  }

}
