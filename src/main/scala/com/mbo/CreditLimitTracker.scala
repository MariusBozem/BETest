package com.mbo

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import org.apache.commons.io.FilenameUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object CreditLimitTracker {

  case class Workbook(name: String, entries: Seq[WorkbookEntry])
  case class WorkbookEntry(
    name: String,
    address: String,
    postcode: String,
    phone: String,
    creditLimit: Double,
    birthday: Option[LocalDate]
  )

  // The route that is available via the Http Server
  // Example: /credit-limit?workbook=Workbook2.csv&workbook=Workbook2.prn
  def route(implicit ec: ExecutionContext, mat: Materializer) = pathPrefix("credit-limit") {
    pathEnd {
      get {
        parameters(Symbol("workbook").*) { (workbooks: Iterable[String]) =>
          onComplete(readWorkbooks(workbooks.toSeq.reverse)) {
            case Success(workbooks: Seq[Workbook]) =>
              complete(
                HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  makeHTMLTemplate(workbooks)
                )
              )
            case Failure(ex) =>
              complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    }
  }

  // The workbooks that this application can access
  val availableWorkbooks = List("Workbook2.csv", "Workbook2.prn")

  // Given a list of workbook names this method filters out invalid workbook names and
  // then delegates to the appropriate reader method based on the file extension
  def readWorkbooks(workbooks: Seq[String])(implicit ec: ExecutionContext, mat: Materializer): Future[Seq[Workbook]] = {
    val futureWorkbooks: Seq[Future[Workbook]] = workbooks.filter { workbookName =>
      availableWorkbooks.contains(workbookName) && Files.exists(Paths.get(workbookName))
    }.map { workbookName =>
      val futureEntries: Future[Seq[WorkbookEntry]] = FilenameUtils.getExtension(workbookName) match {
        case "csv" => readCSV(workbookName)
        case "prn" => readPRN(workbookName)
        case _ => Future.successful(Seq.empty)
      }

      futureEntries.map { entries =>
        Workbook(name = workbookName, entries)
      }
    }

    Future.sequence(futureWorkbooks)
  }

  def readCSV(fileName: String)(implicit mat: Materializer): Future[Seq[WorkbookEntry]] = {
    FileIO.fromPath(Paths.get(fileName))
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
      .map { csvLineAsMap =>
        val birthday = Try(
          LocalDate.parse(csvLineAsMap("Birthday"), csvDateFormatter)
        ).toOption

        WorkbookEntry(
          name = csvLineAsMap("Name").trim,
          address = csvLineAsMap("Address").trim,
          postcode = csvLineAsMap("Postcode").trim,
          phone = csvLineAsMap("Phone").trim,
          creditLimit = csvLineAsMap("Credit Limit").toDouble,
          birthday = birthday
        )
      }
      .runWith(Sink.seq)
  }

  def readPRN(fileName: String)(implicit ec: ExecutionContext, mat: Materializer): Future[Seq[WorkbookEntry]] = {
    FileIO.fromPath(Paths.get(fileName))
      .via(Framing.delimiter(ByteString("\n"), 1024, true))
      .map(_.utf8String)
      .runWith(Sink.seq)
      .map { lines =>
        if (lines.isEmpty) {
          Seq.empty
        } else {
          val header = lines(0)
          val columnLengths = getPRNColumnLengths(header)

          lines.drop(1).map { line =>
            var remainingLine = line
            val columnValues: Seq[String] = columnLengths.map { colLength =>
              val colValue = remainingLine.take(colLength)
              remainingLine = remainingLine.drop(colLength)
              colValue.trim
            }

            val birthday = Try(
              LocalDate.parse(columnValues(5), prnDateFormatter)
            ).toOption

            // NOTE: assumes file with the correct amount of columns; can be adapted to handle invalid cases
            WorkbookEntry(
              name = columnValues(0),
              address = columnValues(1),
              postcode = columnValues(2),
              phone = columnValues(3),
              creditLimit = columnValues(4).toDouble,
              birthday = birthday
            )
          }
        }
      }
  }

  val csvDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  val prnDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  // Since the .prn file does not contain a delimiting character like the CSV file the following logic is used to split the lines into the individual columns:
  // Read the header line to figure out the length of each column (the length of a column is basically the name of the column and all the following whitespace until the next word starts) and then split the content accordingly
  private def getPRNColumnLengths(headerLine: String, colLengths: Seq[Int] = Seq.empty): Seq[Int] = {
    if (headerLine.isEmpty) colLengths
    else {
      val l1 = headerLine.takeWhile(!_.isWhitespace)

      // handle the special case of the "Credit Limit" column which consists of 2 words
      val l2 = if (l1 == "Credit") {
        " Limit" + headerLine.drop("Credit Limit".length).takeWhile(_.isWhitespace)
      } else {
        headerLine.drop(l1.length).takeWhile(_.isWhitespace)
      }

      val columnLen = l1.length + l2.length
      val remainder = headerLine.drop(columnLen)

      getPRNColumnLengths(remainder, colLengths :+ columnLen)
    }
  }

  def makeHTMLTemplate(workbooks: Seq[Workbook]): String = {
    import scalatags.Text.all._

    val noWorkbookMessage = if (workbooks.isEmpty) "No workbook was specified. Access workbooks by adding query parameters to the URL. For example '?workbook=Workbook2.csv'" else ""

    html(
      head(),
      body(
        h1("Workbooks"),
        noWorkbookMessage,
        workbooks.map { workbook =>
          div(
            h3(workbook.name),
            table(
              thead(
                tr(
                  th("Name"),
                  th("Address"),
                  th("Postcode"),
                  th("Phone"),
                  th("Credit Limit"),
                  th("Birthday")
                )
              ),
              tbody(
                workbook.entries.map { entry =>
                  val birthday = entry.birthday
                    .map(_.format(DateTimeFormatter.ISO_DATE))
                    .getOrElse("Unknown")

                  tr(
                    td(entry.name),
                    td(entry.address),
                    td(entry.postcode),
                    td(entry.phone),
                    td(textAlign:="right", String.format("%.2f", entry.creditLimit)),
                    td(birthday)
                  )
                }
              )
            )
          )
        }
      )
    ).toString()
  }

}
