# BETest

## Introduction

Small proof of concept for an app that tracks credit limits from several sources (e.g. CSV and PRN). The solution has been implemented using Scala with the Akka Http library for delivering the HTML, Akka Streams for reading and parsing the Workbook files and a small library (scalatags) to create the HTML.

## Prerequisites

- JDK 8 or newer
- sbt (for building and running the Scala code)

## Running the app

To start the app simply run "sbt run" in the root folder.

Stop it by pressing ENTER.

## Using the app

Visit http://localhost:8080/credit-limit?workbook=Workbook2.csv&workbook=Workbook2.prn

The app exposes 1 endpoint at the '/credit-limit' url. The workbooks are specified in the 'workbook' query parameters.

## Testing the app

Tests are in the 'CreditLimitTrackerSpec.scala' file.
To run them run "sbt test" in the root folder

## Technical documentation

The app consists of 2 Scala files:
- "HttpServer.scala" starts and runs the Http Server until the user stops the app.
- "CreditLimitTracker.scala" contains the code for the route and the logic required for reading and parsing the different workbook formats.
