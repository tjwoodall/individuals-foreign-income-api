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

package v2.controllers.validators.resolvers

import cats.data.Validated.{Invalid, Valid}
import shared.utils.UnitSpec
import v1.models.domain.CustomerRef
import v1.models.errors.CustomerRefFormatError

class ResolveCustomerRefSpec extends UnitSpec {

  private val path: String             = "some-path"
  private val validCustomerRef: String = "PENSIONINCOME245"

  "CustomerRefValidation" when {
    "validate" must {
      "return an empty list for a valid customerRef" in {

        val result = ResolveCustomerRef(validCustomerRef, path)
        result shouldBe Valid(CustomerRef(validCustomerRef))
      }

      "return a CustomerRefFormatError for an invalid customerRef" in {

        val customerRef: String = "This customer ref string is 91 characters long ------------------------------------------91"
        val result              = ResolveCustomerRef(customerRef, path)
        result shouldBe Invalid(List(CustomerRefFormatError.withPath(path)))
      }
    }

    "validateOptional" must {
      "return an empty list for a value of 'None'" in {
        val customerRef = None
        val result      = ResolveCustomerRef(customerRef, path)
        result shouldBe Valid(None)
      }

      "validate correctly for some valid customerRef" in {
        val customerRef: Option[String] = Some(validCustomerRef)
        val result                      = ResolveCustomerRef(customerRef, path)
        result shouldBe Valid(Some(CustomerRef(validCustomerRef)))
      }

      "validate correctly for some invalid customerRef" in {

        val customerRef = Some("This customer ref string is 91 characters long ------------------------------------------91")
        val result      = ResolveCustomerRef(customerRef, path)
        result shouldBe Invalid(List(CustomerRefFormatError.withPath(path)))
      }
    }
  }

}
