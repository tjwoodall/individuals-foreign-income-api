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

package v2.controllers.validators

import config.{ForeignIncomeConfig, MockForeignIncomeConfig}
import shared.controllers.validators.Validator
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v2.models.request.retrieve.RetrieveForeignRequest

class RetrieveForeignValidatorFactorySpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2020-21"

  class Test extends MockForeignIncomeConfig {
    implicit val correlationId: String = "1234"

    implicit val appConfig: ForeignIncomeConfig = mockForeignIncomeConfig

    MockedForeignIncomeConfig
      .minimumPermittedTaxYear()
      .returns(2021)
      .anyNumberOfTimes()

    val validatorFactory                                                            = new RetrieveForeignValidatorFactory()
    def validator(nino: String, taxYear: String): Validator[RetrieveForeignRequest] = validatorFactory.validator(nino, taxYear)

  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        val result = validator(validNino, validTaxYear).validateAndWrapResult()
        result shouldBe Right(RetrieveForeignRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear)))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        val result = validator("A12344A", validTaxYear).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        val result = validator(validNino, "20178").validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year range is supplied" in new Test {
        val result = validator(validNino, "2019-21").validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an invalid tax year is supplied" in new Test {
        val result = validator(validNino, "2018-19").validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in new Test {
        val result = validator("A12344A", "20178").validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
