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
import cats.data.Validated.Valid
import cats.implicits.toFoldableOps
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.{ResolveParsedCountryCode, ResolveParsedNumber}
import shared.models.errors.MtdError
import v2.controllers.validators.resolvers.ResolveCustomerRef
import v2.models.request.createAmend.{CreateAmendForeignRequest, ForeignEarnings, UnremittableForeignIncomeItem}

object CreateAmendForeignValidator extends RulesValidator[CreateAmendForeignRequest] {

  def validateBusinessRules(parsed: CreateAmendForeignRequest): Validated[Seq[MtdError], CreateAmendForeignRequest] = {
    import parsed.body

    val foreignEarningsValidation: Validated[Seq[MtdError], Unit] = validateForeignEarnings(body.foreignEarnings)

    val unremittableForeignIncomeValidations: Validated[Seq[MtdError], Unit] = validateUnremittableForeignIncome(body.unremittableForeignIncome)
    combine(foreignEarningsValidation, unremittableForeignIncomeValidations).onSuccess(parsed)
  }

  private def validateForeignEarnings(foreignEarnings: Option[ForeignEarnings]): Validated[Seq[MtdError], Unit] = {
    foreignEarnings match {
      case Some(earnings) =>
        combine(
          ResolveCustomerRef(earnings.customerReference, s"/foreignEarnings/customerReference"),
          ResolveParsedNumber().apply(earnings.earningsNotTaxableUK, s"/foreignEarnings/earningsNotTaxableUK")
        )
      case None =>
        Valid(())
    }
  }

  private def validateUnremittableForeignIncome(
      unremittableForeignIncome: Option[Seq[UnremittableForeignIncomeItem]]): Validated[Seq[MtdError], Unit] = {
    unremittableForeignIncome match {
      case Some(items) if items.nonEmpty =>
        items.zipWithIndex.traverse_ { case (item, index) =>
          validateUnremittableForeignIncomeItem(item, index)
        }
      case _ =>
        Valid(())
    }
  }

  private def validateUnremittableForeignIncomeItem(item: UnremittableForeignIncomeItem, index: Int): Validated[Seq[MtdError], Unit] = {
    combine(
      ResolveParsedCountryCode(item.countryCode, s"/unremittableForeignIncome/$index/countryCode"),
      ResolveParsedNumber().apply(item.amountInForeignCurrency, s"/unremittableForeignIncome/$index/amountInForeignCurrency"),
      ResolveParsedNumber().apply(item.amountTaxPaid, s"/unremittableForeignIncome/$index/amountTaxPaid")
    )
  }

}
