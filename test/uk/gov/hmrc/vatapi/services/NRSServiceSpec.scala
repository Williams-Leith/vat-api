/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.vatapi.services

import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatapi.UnitSpec
import uk.gov.hmrc.vatapi.assets.TestConstants.Auth.orgAuthContextWithNrsData
import uk.gov.hmrc.vatapi.assets.TestConstants.NRSResponse._
import uk.gov.hmrc.vatapi.assets.TestConstants.VatReturn._
import uk.gov.hmrc.vatapi.httpparsers.NrsError
import uk.gov.hmrc.vatapi.httpparsers.NrsSubmissionHttpParser.NrsSubmissionOutcome
import uk.gov.hmrc.vatapi.mocks.connectors.MockNRSConnector
import uk.gov.hmrc.vatapi.models.VatReturnDeclaration
import uk.gov.hmrc.vatapi.resources.AuthRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NRSServiceSpec extends UnitSpec with GuiceOneAppPerSuite with MockFactory with ScalaFutures with MockNRSConnector {

  val testNRSService = new NRSService(mockNRSConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val req: AuthRequest[_] = new AuthRequest(orgAuthContextWithNrsData, FakeRequest().withHeaders(("Authorization", "Bearer test-bearer-token")))

  val testDateTime: DateTime = DateTime.now()

  "NRSService.submit" when {

    lazy val testVrn: Vrn = Vrn("123456789")
    def result(declaration: VatReturnDeclaration): Future[NrsSubmissionOutcome] = {
      val submission = testNRSService.convertToNrsSubmission(testVrn, declaration)
      testNRSService.submit(testVrn, submission)
    }

    "successful responses are returned from the connector" should {
      "return the correctly formatted NRS Data model" in {
        setupNrsSubmission(testVrn, nrsSubmission)(Right(nrsClientData))
        extractAwait(result(vatReturnDeclaration)) shouldBe Right(nrsClientData)
      }
    }

    "error responses are returned from the connector" should {
      "return an NRS Error model" in {
        setupNrsSubmission(testVrn, nrsSubmission)(Left(NrsError))
        extractAwait(result(vatReturnDeclaration)) shouldBe Left(NrsError)
      }
    }
  }
}
