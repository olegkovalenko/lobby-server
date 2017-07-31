name := "lobby"

organization := "com.evolutiongaming"

version := "0.0.1"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  val akkaVersion = "2.5.3"
  val akkaHttpVersion = "10.0.9"
  val logbackVersion = "1.1.7"
  val scalaTestVersion = "3.0.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,

    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  )
}

fork in run := true
