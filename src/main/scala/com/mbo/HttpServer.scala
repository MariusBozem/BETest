package com.mbo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.ExceptionHandler

import scala.io.StdIn

object HttpServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("credit-limit-tracker-system")
    implicit val executionContext = system.dispatcher

    // Start and run the Akka HTTP server
    val route = CreditLimitTracker.route
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
