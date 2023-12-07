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

package v1.controllers.requestParsers.validators

import config.AppConfig
import shared.controllers.requestParsers.validators.Validator
import shared.controllers.requestParsers.validators.validations._
import shared.models.errors.MtdError
import v1.controllers.requestParsers.validators.validations.CustomerRefValidation
import v1.models.request.createAmend.{CreateAmendForeignRawData, CreateAmendForeignRequestBody, ForeignEarnings, UnremittableForeignIncomeItem}

import javax.inject.{Inject, Singleton}

@Singleton
class CreateAmendForeignValidator @Inject() (implicit val appConfig: AppConfig) extends Validator[CreateAmendForeignRawData] with ValueFormatErrorMessages {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation, bodyFormatValidator, bodyValueValidator)

  override def validate(data: CreateAmendForeignRawData): Seq[MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: CreateAmendForeignRawData => Seq[Seq[MtdError]] = (data: CreateAmendForeignRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: CreateAmendForeignRawData => Seq[Seq[MtdError]] = (data: CreateAmendForeignRawData) => {
    List(
      TaxYearNotSupportedValidation.validate(data.taxYear, appConfig.minimumPermittedTaxYear)
    )
  }

  private def bodyFormatValidator: CreateAmendForeignRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[CreateAmendForeignRequestBody](data.body.json)
    )
  }

  private def bodyValueValidator: CreateAmendForeignRawData => Seq[Seq[MtdError]] = { data =>
    val requestBodyData = data.body.json.as[CreateAmendForeignRequestBody]

    List(
      flattenErrors(
        List(
          requestBodyData.foreignEarnings.map { data => validateForeignEarnings(data) }.getOrElse(NoValidationErrors),
          requestBodyData.unremittableForeignIncome
            .map(_.zipWithIndex.flatMap { case (data, index) =>
              validateUnremittableForeignIncome(data, index)
            })
            .getOrElse(NoValidationErrors)
            .toList
        )
      ))
  }

  private def validateForeignEarnings(foreignEarnings: ForeignEarnings): Seq[MtdError] = {
    List(
      foreignEarnings.customerReference.fold(NoValidationErrors: Seq[MtdError]) { ref =>
        CustomerRefValidation
          .validate(ref)
          .map(
            _.copy(paths = Some(List(s"/foreignEarnings/customerReference")))
          )
      },
      DecimalValueValidation.validate(amount = foreignEarnings.earningsNotTaxableUK, path = s"/foreignEarnings/earningsNotTaxableUK")
    ).flatten
  }

  private def validateUnremittableForeignIncome(unremittableForeignIncome: UnremittableForeignIncomeItem, arrayIndex: Int): Seq[MtdError] = {
    List(
      CountryCodeValidation
        .validate(unremittableForeignIncome.countryCode)
        .map(
          _.copy(paths = Some(List(s"/unremittableForeignIncome/$arrayIndex/countryCode")))
        ),
      DecimalValueValidation.validate(
        amount = unremittableForeignIncome.amountInForeignCurrency,
        path = s"/unremittableForeignIncome/$arrayIndex/amountInForeignCurrency"),
      DecimalValueValidation.validateOptional(
        amount = unremittableForeignIncome.amountTaxPaid,
        path = s"/unremittableForeignIncome/$arrayIndex/amountTaxPaid")
    ).flatten
  }

}
