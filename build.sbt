// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

ThisBuild / version := "0.11.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / crossScalaVersions := Vector("2.12.8", "2.13.0-M5", "2.11.12", "2.10.7")

lazy val root = (project in file("."))
  .aggregate(expectyJVM, expectyJS)
  .settings(
    name := "Expecty Root",
    Compile / sources := Nil,
    skip in publish := true,
  )

lazy val expecty = crossProject(JVMPlatform, JSPlatform, NativePlatform).in(file("."))
  .settings(
    name := "Expecty",
    scalacOptions ++= {
      if (scalaVersion.value startsWith "2.10") Nil
      else Seq("-Yrangepos", "-feature", "-deprecation")
    },
    Compile / unmanagedSourceDirectories ++= {
      if (scalaVersion.value startsWith "2.13") Seq((baseDirectory in LocalRootProject).value / "shared" / "src" / "main" / "scala-2.13-beta")
      else Nil
    },
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.6" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
  .jvmSettings(
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
  )
  .jsSettings(
    scalaJSModuleKind := ModuleKind.CommonJSModule,
  )
  .nativeSettings(
    nativeLinkStubs := true
  )

lazy val expectyJVM    = expecty.jvm
lazy val expectyJS     = expecty.js
lazy val expectyNative = expecty.native
