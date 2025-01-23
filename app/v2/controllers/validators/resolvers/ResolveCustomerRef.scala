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

import cats.data.Validated
import shared.controllers.validators.resolvers.{ResolveStringPattern, ResolverSupport}
import shared.models.errors.MtdError
import v2.models.domain.CustomerRef
import v2.models.errors.CustomerRefFormatError

import scala.util.matching.Regex

object ResolveCustomerRef extends ResolverSupport {
  private val regex: Regex = "^[0-9a-zA-Z{À-˿'}\\- _&`():.'^]{1,90}$".r

  def resolver(error: => MtdError): Resolver[String, CustomerRef] =
    ResolveStringPattern(regex, error).resolver.map(CustomerRef)

  def apply(value: Option[String], path: String): Validated[Seq[MtdError], Option[CustomerRef]] =
    resolver(errorFor(path)).resolveOptionally(value)

  def apply(value: String, path: String): Validated[Seq[MtdError], CustomerRef] =
    resolver(errorFor(path))(value)

  private def errorFor(path: String): MtdError = CustomerRefFormatError.withPath(path)
}
