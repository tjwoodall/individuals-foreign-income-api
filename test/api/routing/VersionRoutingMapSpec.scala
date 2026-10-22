/*
 * Copyright 2026 HM Revenue & Customs
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

package api.routing

import api.utils.UnitSpec
import play.api.mvc.Handler
import play.api.routing.Router
import play.api.routing.sird._

class VersionRoutingMapSpec extends UnitSpec {

  object DefaultHandler extends Handler
  object V2Handler      extends Handler

  private val defaultRoute = Router.from { case GET(p"") =>
    DefaultHandler
  }

  private val v2Router = Router.from { case GET(p"/v2") =>
    V2Handler
  }

  private val routingMap = new VersionRoutingMap {
    override val defaultRouter: Router = defaultRoute

    override val map: Map[Version, Router] = Map(
      Version2 -> v2Router
    )

  }

  "VersionRoutingMap" should {
    "return the router for Version 2" in {
      routingMap.versionRouter(Version2) shouldBe Some(v2Router)
    }

    "return None for an unsupported version" in {
      routingMap.versionRouter(Version9) shouldBe None
    }
  }

}
