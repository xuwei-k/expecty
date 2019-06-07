// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

ThisBuild / version := "0.11.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / crossScalaVersions := Vector("2.12.8", "2.13.0", "2.11.12", "2.10.7")

lazy val root = (project in file("."))
  .aggregate(expectyJVM, expectyJS)
  .settings(
    name := "Expecty Root",
    Compile / sources := Nil,
    skip in publish := true,
  )

lazy val utestVersion = "0.6.6"
lazy val utestJVMRef = ProjectRef(uri("git://github.com/eed3si9n/utest.git#79950544"), "utestJVM")
lazy val utestJVMLib = "com.lihaoyi" %% "utest" % utestVersion
lazy val utestJSRef = ProjectRef(uri("git://github.com/eed3si9n/utest.git#79950544"), "utestJS")
lazy val utestJSLib = "com.lihaoyi" %% "utest_sjs0.6" % utestVersion

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
  //  .sourceDependency(utestJVMRef % Test, utestJVMLib % Test)
lazy val expectyJS     = expecty.js
  //  .sourceDependency(utestJSRef % Test, utestJSLib % Test)
lazy val expectyNative = expecty.native
  // .settings(
  //   libraryDependencies += "com.lihaoyi" %%% "utest" % utestVersion
  // )
