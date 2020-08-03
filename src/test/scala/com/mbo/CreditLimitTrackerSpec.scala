package com.mbo

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.concurrent.ScalaFutures

class CreditLimitTrackerSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  val route = CreditLimitTracker.route

  "The '/credit-limit' route" should {
    "return a valid HTML structure if no workbook is specified" in {
      val expectedHtml = CreditLimitTracker.makeHTMLTemplate(Seq.empty)

      Get("/credit-limit") ~> route ~> check {
        responseAs[String] shouldEqual expectedHtml
      }
    }

    "parse the specified workbooks and return them in HTML format" in {
      val workbooks = CreditLimitTracker.readWorkbooks(Seq("Workbook2.csv", "Workbook2.prn")).futureValue
      val expectedHtml = CreditLimitTracker.makeHTMLTemplate(workbooks)

      Get("/credit-limit?workbook=Workbook2.csv&workbook=Workbook2.prn") ~> route ~> check {
        responseAs[String] shouldEqual expectedHtml
      }
    }
  }

  "Reading the workbooks" should {
    "return an empty list if no workbook is given" in {
      val result = CreditLimitTracker.readWorkbooks(Seq.empty).futureValue
      result.isEmpty shouldBe true
    }

    "filter out all invalid workbooks" in {
      val invalidWorkbooks = Seq("asd.csv", "Workbook3.csv", "Workbook3.prn")
      val result = CreditLimitTracker.readWorkbooks(invalidWorkbooks).futureValue
      result.isEmpty shouldBe true
    }

    "read and properly parse workbooks in CSV format" in {
      val workbooks = Seq("Workbook2.csv")
      val result = CreditLimitTracker.readWorkbooks(workbooks).futureValue

      result.size shouldBe 1
      val workbook = result.head
      workbook.name shouldBe "Workbook2.csv"
      workbook.entries.size shouldBe 7

      val sampleEntry = workbook.entries.head
      sampleEntry.name shouldBe "Aloys, Schwarz"
      sampleEntry.address shouldBe "Lindenweg 279"
      sampleEntry.postcode shouldBe "14547"
      sampleEntry.phone shouldBe "0222131/24557853"
      sampleEntry.creditLimit shouldBe 15000.00D
      sampleEntry.birthday shouldBe Some(LocalDate.of(1965, 1, 1))
    }

    "read and properly parse workbooks in PRN format" in {
      val workbooks = Seq("Workbook2.prn")
      val result = CreditLimitTracker.readWorkbooks(workbooks).futureValue

      result.size shouldBe 1
      val workbook = result.head
      workbook.name shouldBe "Workbook2.prn"
      workbook.entries.size shouldBe 7

      val sampleEntry = workbook.entries.head
      sampleEntry.name shouldBe "Aloys, Schwarz"
      sampleEntry.address shouldBe "Lindenweg 279"
      sampleEntry.postcode shouldBe "14547"
      sampleEntry.phone shouldBe "0222131/24557"
      sampleEntry.creditLimit shouldBe 15000.00D
      sampleEntry.birthday shouldBe Some(LocalDate.of(1965, 1, 1))
    }
  }

}
