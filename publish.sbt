ThisBuild / organization := "com.eed3si9n.expecty"
ThisBuild / organizationName := "eed3si9n"
ThisBuild / organizationHomepage := Some(url("http://eed3si9n.com/"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/eed3si9n/expecty"), "git@github.com:eed3si9n/expecty.git"))
ThisBuild / developers := List(
  Developer("pniederw", "Peter Niederwieser", "@pniederw", url("https://github.com/pniederw")),
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n")),
)
ThisBuild / description := "Power assertions (as known from Groovy and Spock) for the Scala language."
ThisBuild / licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/eed3si9n/expecty"))

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
