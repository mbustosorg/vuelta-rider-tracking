/*

    Copyright (C) 2015 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

packageArchetype.java_application

lazy val commonSettings = Seq(
   organization := "org.bustos",
   version := "0.1.0",
   scalaVersion := "2.11.4"
)

lazy val vueltaRiderTracker = (project in file("."))
    .settings(name := "vueltaRiderTracker")
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= projectLibraries)
    .settings(resolvers += "Spray" at "http://repo.spray.io")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val slf4jV = "1.7.6"
val sprayV = "1.3.2"
val akkaV = "2.3.6"

val projectLibraries = Seq(
    "io.spray"                %% "spray-can"       % sprayV,
    "io.spray"                %% "spray-routing"   % sprayV,
    "io.spray"                %% "spray-testkit"   % sprayV  % "test",
    "io.spray"                %% "spray-json"      % "1.3.1",
    "com.typesafe.akka"       %% "akka-actor"      % akkaV,
    "com.typesafe.akka"       %% "akka-testkit"    % akkaV   % "test",
    "com.typesafe.slick"      %% "slick"           % "2.1.0",
    "org.seleniumhq.selenium" %  "selenium-java"   % "2.35.0",
    "org.scalatest"           %% "scalatest"       % "2.1.6",
    "org.specs2"              %% "specs2-core"     % "2.3.11" % "test",
    "com.wandoulabs.akka"     %% "spray-websocket" % "0.1.3",
    "com.gettyimages"         %% "spray-swagger"   % "0.5.0",
    "log4j"                   %  "log4j"           % "1.2.14",
    "org.slf4j"               %  "slf4j-api"       % slf4jV,
    "org.slf4j"               %  "slf4j-simple"    % slf4jV,
    "mysql"                   %  "mysql-connector-java" % "latest.release",
    "joda-time"               %  "joda-time"       % "2.7",
    "org.joda"                %  "joda-convert"    % "1.2",
    "com.github.tototoshi"    %% "scala-csv"       % "1.2.2"
)

Revolver.settings