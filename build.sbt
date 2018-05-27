ThisBuild / version := "0.11.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.6"
ThisBuild / crossScalaVersions := Vector("2.12.6", "2.13.0-M4", "2.11.12", "2.10.6")

lazy val expecty = (project in file("."))
  .settings(
    name := "Expecty",

    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    scalacOptions ++= {
      if (scalaVersion.value startsWith "2.10") Nil
      else Seq("-Yrangepos", "-feature", "-deprecation")
    },
    Compile / unmanagedSourceDirectories ++= {
      if (scalaVersion.value startsWith "2.13") Seq(baseDirectory.value / "src" / "main" / "scala-2.13-beta")
      else Nil
    },
  )
