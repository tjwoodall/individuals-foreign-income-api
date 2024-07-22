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

package v1.controllers.validators

import config.{ForeignIncomeConfig, MockForeignIncomeConfig}
import play.api.libs.json.{JsValue, Json}
import shared.UnitSpec
import shared.controllers.validators.Validator
import shared.controllers.validators.validations.ValueFormatErrorMessages
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import v1.models.errors.{CountryCodeRuleError, CustomerRefFormatError}
import v1.models.request.createAmend.{CreateAmendForeignRequest, CreateAmendForeignRequestBody, ForeignEarnings, UnremittableForeignIncomeItem}

class CreateAmendForeignValidatorSpec extends UnitSpec with MockForeignIncomeConfig with ValueFormatErrorMessages {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2018-19"

  private val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"0"
      |     },
      |     {
      |       "countryCode":"GBR",
      |       "amountInForeignCurrency":"99999999999.99",
      |       "amountTaxPaid":"99999999999.99"
      |     },
      |     {
      |       "countryCode":"ESP",
      |       "amountInForeignCurrency":"0.99",
      |       "amountTaxPaid":"100"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val unremittableForeignIncome: Seq[UnremittableForeignIncomeItem] = Seq(
    UnremittableForeignIncomeItem("FRA", 0, Some(0)),
    UnremittableForeignIncomeItem("GBR", 99999999999.99, Some(99999999999.99)),
    UnremittableForeignIncomeItem("ESP", 0.99, Some(100.00))
  )

  private val createAmendForeignRequest: CreateAmendForeignRequest = CreateAmendForeignRequest(
    Nino(validNino),
    TaxYear.fromMtd(validTaxYear),
    CreateAmendForeignRequestBody(Some(ForeignEarnings(Some("FOREIGNINCME123A"), 99999999999.99)), Some(unremittableForeignIncome))
  )

  private val allInvalidValueRawRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"This customer ref string is 91 characters long ------------------------------------------91",
      |     "earningsNotTaxableUK":"99999999999.9999999"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRFFFA",
      |       "amountInForeignCurrency":"-1000",
      |       "amountTaxPaid":"99999999999999999999999"
      |     },
      |     {
      |       "countryCode":"FFF",
      |       "amountInForeignCurrency":"-1000",
      |       "amountTaxPaid":"99999999999999999999999"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":true
      |     },
      |     {
      |       "countryCode":"GBR",
      |       "amountInForeignCurrency":"99999999999.99",
      |       "amountTaxPaid":false
      |     },
      |     {
      |       "countryCode":"ESP",
      |       "amountInForeignCurrency":"0.99",
      |       "amountTaxPaid":"100"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val missingMandatoryFieldJson: JsValue = Json.parse(
    """
      |{
      |  "unremittableForeignIncome" : [
      |    {
      |      "amountInForeignCurrency":"0",
      |      "amountTaxPaid": 100
      |    }
      |  ]
      |}
    """.stripMargin
  )

  private val invalidEarningsNotTaxableUKRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "earningsNotTaxableUK":"-1"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"0"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val invalidCustomerReferenceRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"This customer ref string is 91 characters long ------------------------------------------91",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"0"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val invalidCountryCodeRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"EEE",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"0"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val invalidCountryCodeFormatRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"EEEE",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"0"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val invalidAmountInForeignCurrencyRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"-1",
      |       "amountTaxPaid":"0"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  private val invalidAmountTaxPaidRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "foreignEarnings": {
      |     "customerReference":"FOREIGNINCME123A",
      |     "earningsNotTaxableUK":"99999999999.99"
      |   },
      |   "unremittableForeignIncome": [
      |     {
      |       "countryCode":"FRA",
      |       "amountInForeignCurrency":"0",
      |       "amountTaxPaid":"99999999999999999999999"
      |     }
      |   ]
      |}
      |""".stripMargin
  )

  class Test extends MockForeignIncomeConfig {
    implicit val correlationId: String = "1234"

    implicit val appConfig: ForeignIncomeConfig = mockForeignIncomeConfig

    MockedForeignIncomeConfig
      .minimumPermittedTaxYear()
      .returns(2019)
      .anyNumberOfTimes()

    val validatorFactory = new CreateAmendForeignValidatorFactory()

    def validator(nino: String, taxYear: String, body: JsValue): Validator[CreateAmendForeignRequest] =
      validatorFactory.validator(nino, taxYear, body)

  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        val result = validator(validNino, validTaxYear, validRequestBodyJson).validateAndWrapResult()
        result shouldBe Right(createAmendForeignRequest)
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {

        val result = validator("A12344A", validTaxYear, validRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        val result = validator(validNino, "20178", validRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return RuleTaxYearNotSupported error" when {
      "an invalid tax year is supplied" in new Test {
        val result = validator(validNino, "2017-18", validRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in new Test {
        val result = validator(validNino, validTaxYear, emptyRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "a non-empty JSON body is submitted without any expected fields" in new Test {
        val result = validator(validNino, validTaxYear, nonsenseRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "the submitted request body is not in the correct format" in new Test {
        val result = validator(validNino, validTaxYear, nonValidRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            RuleIncorrectOrEmptyBodyError.copy(paths = Some(
              List(
                "/unremittableForeignIncome/0/amountTaxPaid",
                "/unremittableForeignIncome/1/amountTaxPaid"
              )))
          ))
      }

      "the submitted request body has missing mandatory fields" in new Test {
        val result = validator(validNino, validTaxYear, missingMandatoryFieldJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            RuleIncorrectOrEmptyBodyError.copy(paths = Some(List(
              "/unremittableForeignIncome/0/countryCode"
            )))))
      }
    }

    "return CustomerRefFormatError error" when {
      "an incorrectly formatted customer reference is submitted" in new Test {
        val result = validator(validNino, validTaxYear, invalidCustomerReferenceRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, CustomerRefFormatError.copy(paths = Some(List("/foreignEarnings/customerReference")))))
      }
    }

    "return ValueFormatError error" when {
      "an incorrectly formatted earningsNotTaxableUK is submitted" in new Test {
        val result = validator(validNino, validTaxYear, invalidEarningsNotTaxableUKRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            ValueFormatError.copy(
              paths = Some(List("/foreignEarnings/earningsNotTaxableUK")),
              message = ZERO_MINIMUM_INCLUSIVE
            )))
      }
    }

    "return ValueFormatError error (single failure)" when {
      "one field fails value validation (countryCode 3 digit)" in new Test {
        val result = validator(validNino, validTaxYear, invalidCountryCodeRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            CountryCodeRuleError.copy(
              paths = Some(List("/unremittableForeignIncome/0/countryCode"))
            )))
      }

      "one field fails value validation (countryCode 4 digit)" in new Test {
        val result = validator(validNino, validTaxYear, invalidCountryCodeFormatRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            CountryCodeFormatError.copy(
              paths = Some(List("/unremittableForeignIncome/0/countryCode"))
            )))
      }

      "one field fails value validation (amountInForeignCurrency)" in new Test {
        val result = validator(validNino, validTaxYear, invalidAmountInForeignCurrencyRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            ValueFormatError.copy(
              message = ZERO_MINIMUM_INCLUSIVE,
              paths = Some(List("/unremittableForeignIncome/0/amountInForeignCurrency"))
            )))
      }

      "one field fails value validation (AmountTaxPaid)" in new Test {
        val result = validator(validNino, validTaxYear, invalidAmountTaxPaidRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            ValueFormatError.copy(
              message = ZERO_MINIMUM_INCLUSIVE,
              paths = Some(List("/unremittableForeignIncome/0/amountTaxPaid"))
            )))
      }
    }

    "return ValueFormatError error (multiple failures)" when {
      "multiple fields fail value validation" in new Test {
        val result = validator(validNino, validTaxYear, allInvalidValueRawRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(
              CountryCodeFormatError.copy(
                paths = Some(List("/unremittableForeignIncome/0/countryCode"))
              ),
              CustomerRefFormatError.copy(
                paths = Some(List("/foreignEarnings/customerReference"))
              ),
              ValueFormatError.copy(
                paths = Some(List(
                  "/foreignEarnings/earningsNotTaxableUK",
                  "/unremittableForeignIncome/0/amountInForeignCurrency",
                  "/unremittableForeignIncome/0/amountTaxPaid",
                  "/unremittableForeignIncome/1/amountInForeignCurrency",
                  "/unremittableForeignIncome/1/amountTaxPaid"
                )),
                message = ZERO_MINIMUM_INCLUSIVE
              ),
              CountryCodeRuleError.copy(
                paths = Some(List("/unremittableForeignIncome/1/countryCode"))
              )
            ))
          ))
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors (path parameters)" in new Test {
        val result = validator("A12344A", "20178", validRequestBodyJson).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
