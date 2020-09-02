lazy val akkaHttpVersion = "10.1.12"
lazy val akkaVersion    = "2.6.5"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.hatmini",
      scalaVersion    := "2.13.1"
    )),
    name := "hat-service",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8",
     "ch.megard" %% "akka-http-cors" % "1.0.0",

      //third-party libs
      //json deserialising code
      "io.circe" %% "circe-parser" % "0.12.3",
      "io.circe" %% "circe-generic" % "0.12.3",
      "io.circe" %% "circe-core" % "0.12.3",
      "de.heikoseeberger" %% "akka-http-circe" % "1.29.1",
      //auth utils
      "com.emarsys" %% "jwt-akka-http" % "1.2.0",
        "com.pauldijou" %% "jwt-core" % "4.2.0",
        "com.pauldijou" %% "jwt-circe" % "4.2.0",
      
      //mongo
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0",

      "com.auth0" % "jwks-rsa" % "0.6.1",
      //cors-utility
      "ch.megard" %% "akka-http-cors" % "1.0.0",

      //logging utilities
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )
