name := """hello-akka"""

version := "1.0"

scalaVersion := "2.11.6"

lazy val akkaVersion = "2.4.0"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers ++= Seq(
  "Spray Repository" at "http://repo.spray.io",
  "Apache Staging" at "https://repository.apache.org/content/repositories/staging/",
  "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.11.9"
)

libraryDependencies ++= {
  val akkaVersion  = "2.3.10"
  val sprayVersion = "1.3.3"
  val sparkVersion = "1.3.0"
  Seq(
    "com.typesafe.akka"  %%  "akka-actor"             % akkaVersion,
    "io.spray"           %%  "spray-can"              % sprayVersion,
    "io.spray"           %%  "spray-routing"          % sprayVersion,
    "io.spray"           %%  "spray-json"             % "1.3.1",
    "io.spray"           %%  "spray-httpx"            % sprayVersion,
    "io.spray"           %%  "spray-testkit"          % sprayVersion % "test",
    "org.specs2"         %%  "specs2-core"            % "3.6" % "test",
    "com.typesafe.akka"  %%  "akka-testkit"           % akkaVersion % "test"

  )
}

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")


fork in run := true