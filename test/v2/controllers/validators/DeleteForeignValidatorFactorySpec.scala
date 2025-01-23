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

import config.{ForeignIncomeConfig, MockForeignIncomeConfig}
import shared.controllers.validators.Validator
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import shared.utils.UnitSpec
import v2.models.request.delete.DeleteForeignRequest

class DeleteForeignValidatorFactorySpec extends UnitSpec {

  val validNino: String = "AA123456B"
  val parsedNino: Nino  = Nino(validNino)

  val validTaxYear: String   = "2019-20"
  val parsedTaxYear: TaxYear = TaxYear.fromMtd(validTaxYear)

  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockForeignIncomeConfig {

    implicit val appConfig: ForeignIncomeConfig = mockForeignIncomeConfig

    MockedForeignIncomeConfig
      .minimumPermittedTaxYear()
      .returns(2019)
      .anyNumberOfTimes()

    val validatorFactory = new DeleteForeignValidatorFactory()

    def validator(nino: String, taxYear: String): Validator[DeleteForeignRequest] = validatorFactory.validator(nino, taxYear)

  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {

        val result = validator(validNino, validTaxYear).validateAndWrapResult()
        result shouldBe Right(DeleteForeignRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear)))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        val result = validator(s"x$validNino", validTaxYear).validateAndWrapResult()
        result shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "multiple validation errors occur" in new Test {
        val result = validator(s"x$validNino", s"x$validTaxYear").validateAndWrapResult()
        result shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
