name := "RestfulApiTest"

scalaVersion := "2.11.6"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")


resolvers ++= Seq(
  "spray nightlies repo" at "http://nightlies.spray.io",
  "spray repo" at "http://repo.spray.io/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val sprayV = "1.3.2"
  val akkaV = "2.3.9"
  Seq(
    "io.spray" %%  "spray-json" % "1.3.2",
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %% "spray-client" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test"
  )
}