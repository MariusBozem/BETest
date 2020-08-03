lazy val akkaHttpVersion = "10.1.12"
lazy val akkaVersion    = "2.6.8"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.mbo",
      scalaVersion    := "2.13.1"
    )),
    name := "BETest",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.1",
      "commons-io" % "commons-io" % "2.7",
      "com.lihaoyi" %% "scalatags" % "0.9.1",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"      % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )
