ThisBuild / version := "0.14.1-SNAPSHOT"
val scala211 = "2.11.12"
val scala212 = "2.12.12"
val scala213 = "2.13.3"
val scala3   = "3.0.0-M3"
ThisBuild / scalaVersion := scala213
Global / semanticdbEnabled := true

lazy val verify = "com.eed3si9n.verify" %% "verify" % "1.0.0"

lazy val root = (project in file("."))
  .aggregate(expecty.projectRefs: _*)
  .settings(
    name := "Expecty Root",   
    Compile / sources := Nil,
    Test / sources := Nil,
    publish / skip := true,
    commands += Command.command("release") { state =>
      "clean" ::
      "publishSigned" ::
      state
    },
  )

lazy val expecty = (projectMatrix in file("."))
  .settings(
    name := "Expecty",
    scalacOptions ++= {
      if (scalaVersion.value startsWith "2.10") Nil
      else Seq("-Yrangepos", "-feature", "-deprecation")
    },
    Compile / unmanagedSourceDirectories ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) =>
          Seq((baseDirectory in LocalRootProject).value / "src" / "main" / "scala-2.13-beta")
        case _ => Nil
      }
    },
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
      case _ => Nil
    }),
    libraryDependencies += verify % Test,
    testFrameworks += new TestFramework("verify.runner.Framework"),
  )
  .jvmPlatform(scalaVersions = Seq(scala213, scala212, scala211, scala3), settings = Seq(
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    Test / unmanagedSourceDirectories ++= {
      Seq((baseDirectory in LocalRootProject).value / "jvm" / "src" / "test" / "scala")
    },
  ))
  .jsPlatform(scalaVersions = Seq(scala213, scala212, scala3))
  .nativePlatform(scalaVersions = Seq(scala211), settings = Seq(
    bspEnabled := false,
    Test / test := { () },
  ))
