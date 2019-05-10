name := "Trivial"

version := "2.0"

scalaVersion := "2.12.1"

mainClass in Compile := Some("org.mbs3.trivial.Main")

libraryDependencies += "com.typesafe" % "config" % "1.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "10.1.7"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.1"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.1"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

libraryDependencies += "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.6"

libraryDependencies += "org.scalaj" % "scalaj-http_2.12" % "2.3.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.0.1"

enablePlugins(DockerPlugin)

fork in run := true

javaOptions in run += "-Dakka.http.client.parsing.max-content-length=1024m"

javaOptions in run += "-Dakka.http.parsing.max-content-length=infinite"

javaOptions in run += "-Dakka.http.client.parsing.max-content-length=infinite"

javaOptions in run += "-Dakka.http.server.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.server.akka.http.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.server.akka.http.client.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.server.akka.http.server.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.client.akka.http.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.client.akka.http.client.parsing.max-content-length=infinite"

javaOptions in run += "-Dplay.client.akka.http.server.parsing.max-content-length=infinite"

dockerfile in docker := {
  val jarFile = artifactPath.in(Compile, packageBin).value

  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"

  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName).mkString(":") + ":" + jarTarget
  new Dockerfile {
    // Base image
    from("java:8")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

imageNames in docker := Seq(
  ImageName(s"martinb3/trivial-scala:latest"), // Sets the latest tag
  ImageName(
    namespace = Some("martinb3"),
    repository = "trivial-scala",
    tag = Some("v" + version.value)
  ) // Sets a name with a tag that contains the project version
)
